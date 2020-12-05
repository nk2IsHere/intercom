package eu.nk2.intercom.boot

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomException
import eu.nk2.intercom.IntercomMethodBundle
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.api.ProvideIntercom
import eu.nk2.intercom.utils.*
import eu.nk2.intercom.utils.NTuple3
import eu.nk2.intercom.utils.asyncMap
import eu.nk2.intercom.utils.then
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class IntercomProviderBeanPostProcessor(
    private val rabbitProperties: Pair<Mono<Connection>, String>,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor {
    private val logger = LoggerFactory.getLogger(IntercomProviderBeanPostProcessor::class.java)

    private lateinit var channel: Mono<Channel>

    @EventListener fun init(event: ContextRefreshedEvent) {
        channel = rabbitProperties.first.asyncMap { it.createChannel() }
            .cache()
    }

    @EventListener fun dispose(event: ContextClosedEvent) {
        channel.block()?.close()
    }

    private fun makeIntercomRequest(id: String, method: Method, args: Array<Any>): Mono<Any?> =
        channel.asyncMap {
            val queue = it.queueDeclare().queue
            val requestId = UUID.randomUUID().toString()
            val publisherId = id.hashCode()
            it.basicPublish(
                "",
                "${rabbitProperties.second}_request_$publisherId",
                AMQP.BasicProperties.Builder()
                    .correlationId(requestId)
                    .replyTo(queue)
                    .build(),
                intercomMethodBundleSerializer.serialize(IntercomMethodBundle(
                    publisherId = publisherId,
                    methodId = method.name.hashCode() xor method.parameters.map { it.type.name }.hashCode(),
                    parameters = args
                ))
            )

            it then requestId then queue
        }.flatMapMany { (channel, requestId, queue) ->
            Flux.create<NTuple4<Channel, String, Boolean, ByteArray>> { sink ->
                channel.basicConsume(
                    queue,
                    true,
                    { tag, delivery -> sink.next(channel then tag then (delivery.properties.correlationId == requestId) then delivery.body) },
                    { tag -> sink.complete() }
                )
            }
            .subscribeOn(Schedulers.boundedElastic())
        }.filter { (_, _, isRelated, _) -> isRelated }
            .asyncMap { (channel, tag, _, body) ->
                channel.basicCancel(tag)
                body
            }
            .map { Optional.ofNullable(intercomReturnBundleSerializer.deserialize<Any>(it)) }
            .filter { it.isPresent }
            .map { it.get() }
            .defaultIfEmpty(IntercomReturnBundle(error = IntercomError.INTERNAL_ERROR, data = null))
            .doOnNext { if(it.error != null) throw IntercomException(it.error) }
            .flatMap { it.data?.toMono() ?: Mono.empty() }
            .next()

    fun mapProviderField(bean: Any, beanName: String, field: Field) {
        logger.debug("Mapping $beanName's provider field to proxy")
        val id = field.getAnnotation(ProvideIntercom::class.java)?.id
            ?: error("id is required in annotation @ProvideIntercom")

        ReflectionUtils.makeAccessible(field)
        field.set(bean, Proxy.newProxyInstance(bean.javaClass.classLoader, arrayOf(field.type)) { _, method, args ->
            return@newProxyInstance makeIntercomRequest(id, method, args)
                .let {
                    when(method.returnType) {
                        Mono::class.java -> it
                        Flux::class.java -> it.flatMapMany { Flux.fromIterable(it as List<*>) }
                        else -> error("Unknown error")
                    }
                }
        })
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
        return bean.apply {
            ReflectionUtils.doWithFields(
                bean.javaClass,
                { field -> mapProviderField(bean, beanName, field) },
                { field -> field.isAnnotationPresent(ProvideIntercom::class.java) }
            )
        }
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        return super.postProcessAfterInitialization(bean, beanName)
    }
}