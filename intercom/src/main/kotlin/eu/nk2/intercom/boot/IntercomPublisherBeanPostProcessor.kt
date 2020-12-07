package eu.nk2.intercom.boot

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.Delivery
import eu.nk2.intercom.*
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.api.PublishIntercom
import eu.nk2.intercom.utils.*
import eu.nk2.intercom.utils.wrapOptional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.annotation.AnnotationUtils
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class IntercomPublisherBeanPostProcessor(
    private val rabbitProperties: Pair<Mono<Connection>, String>,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor, PriorityOrdered {
    private val log = LoggerFactory.getLogger(IntercomPublisherBeanPostProcessor::class.java)

    private val intercomPublisherAwareBeans = hashMapOf<String, Class<*>>()
    private val intercomPublishers: MutableMap<Int, Pair<Any, Map<Int, Method>>> = ConcurrentHashMap()

    private lateinit var receivers: List<Disposable>

    private fun bootstrapResponseKafkaStream(publisherId: Int): Disposable =
        rabbitProperties.first
            .asyncMap {
                val channel = it.createChannel()
                channel.queueDeclare("${rabbitProperties.second}:$publisherId", false, false, true, null)
                channel.basicQos(1)

                channel
            }
            .flatMapMany { channel ->
                Flux.create<NTuple2<Channel, Delivery>> { sink ->
                    channel.basicConsume(
                        "${rabbitProperties.second}:$publisherId",
                        true,
                        { _, delivery -> sink.next(channel then delivery) },
                        { _ -> sink.complete() }
                    )
                }
                .subscribeOn(Schedulers.boundedElastic())
                .doOnCancel { channel.close() }
            }
            .publish()
            .autoConnect(0)
            .map { (channel, delivery) -> channel then delivery.properties.replyTo then delivery.properties.correlationId then delivery.body }
            .map { (channel, replyKey, key, value) -> channel then replyKey then key then Optional.ofNullable(intercomMethodBundleSerializer.deserialize(value)) }
            .filter { (_, _, _, value) -> value.isPresent }
            .map { (channel, replyKey, key, value) -> channel then replyKey then key then value.get() }
            .doOnNext { (_, _, _, value) -> log.debug("Received from intercom client ${value.publisherId}.${value.methodId}()") }
            .flatMap<NTuple4<Channel, String, String, IntercomReturnBundle<Any?>>> { (channel, replyKey, key, value) ->
                run {
                    val publisherDefinition = intercomPublishers[value.publisherId]
                        ?: return@run Mono.error<Optional<Any>>(IntercomException(BadPublisherIntercomError))

                    val (publisher, method) = publisherDefinition.first to (publisherDefinition.second[value.methodId]
                        ?: return@run Mono.error<Optional<Any>>(IntercomException(BadMethodIntercomError)))

                    if (method.parameterCount != value.parameters.size)
                        return@run Mono.error<Optional<Any>>(IntercomException(BadParamsIntercomError))

                    for ((index, parameter) in method.parameters.withIndex())
                        if (ClassUtils.objectiveClass(parameter.type) != ClassUtils.objectiveClass(value.parameters[index].javaClass))
                            return@run Mono.error<Optional<Any>>(IntercomException(BadParamsIntercomError))

                    return@run try {
                        when (method.returnType) {
                            Mono::class.java -> (method.invoke(publisher, *value.parameters) as Mono<*>)
                                .onErrorResume { Mono.error(IntercomException(ProviderIntercomError(it))) }
                            Flux::class.java -> (method.invoke(publisher, *value.parameters) as Flux<*>)
                                .onErrorResume { Mono.error(IntercomException(ProviderIntercomError(it))) }
                                .collectList()
                            else -> Mono.error<Any>(IntercomException(BadMethodReturnTypeIntercomError))
                        }.wrapOptional()
                    } catch (e: Exception) {
                        log.debug("Error in handling intercom client", e)
                        Mono.error<Optional<Any>>(IntercomException(when (e) {
                            is IllegalArgumentException -> BadDataIntercomError
                            is InvocationTargetException -> ProviderIntercomError(e)
                            else -> InternalIntercomError(e)
                        }))
                    }
                }.map<IntercomReturnBundle<Any?>> { IntercomReturnBundle(error = null, data = it.orNull()) }
                    .onErrorResume(IntercomException::class.java) { Mono.just(IntercomReturnBundle<Any?>(error = it.error, data = null)) }
                    .defaultIfEmpty(IntercomReturnBundle(error = NoDataIntercomError, data = null))
                    .map { channel then replyKey then key then it }
            }
            .map { (channel, replyKey, key, value) -> channel then replyKey then key then intercomReturnBundleSerializer.serialize(value) }
            .asyncMap { (channel, replyKey, key, value) ->
                channel.basicPublish(
                    "",
                    replyKey,
                    AMQP.BasicProperties.Builder()
                        .correlationId(key)
                        .build(),
                    value
                )
            }
            .subscribe()

    @EventListener fun init(event: ContextRefreshedEvent) {
        receivers = intercomPublishers.map { (publisherId, _) -> bootstrapResponseKafkaStream(publisherId) }
            .toList()
    }

    @EventListener fun dispose(event: ContextClosedEvent) {
        receivers.forEach { it.dispose() }
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? =
        bean.apply {
            if (AnnotationUtils.getAnnotation(bean.javaClass, PublishIntercom::class.java) != null) {
                if(bean.javaClass.constructors.none { it.parameters.isEmpty() })
                    error("Classes that contain methods annotated with @PublishIntercom must contain empty constructor, " +
                        "please look at the implementation of ${bean.javaClass.name}")

                intercomPublisherAwareBeans[beanName] = this.javaClass
            }
        }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        intercomPublisherAwareBeans[beanName]?.let { beanClass ->
            val id = AnnotationUtils.getAnnotation(beanClass, PublishIntercom::class.java)?.id
                ?: error("id is required in annotation @PublishIntercom")

            intercomPublishers[id.hashCode()] = bean to (beanClass.methods
                .asSequence()
                .filter { method -> INTERCOM_ALLOWED_GENERIC_METHOD_RETURN_TYPES.any { it.isAssignableFrom(method.returnType) } }
                .map { it.name.hashCode() xor it.parameters.map { it.type.name }.hashCode() to it }
                .toMap())
            log.debug("Mapped publisher $id to registry")
        }

        return super.postProcessAfterInitialization(bean, beanName)
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
