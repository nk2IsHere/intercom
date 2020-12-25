package eu.nk2.intercom.boot

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import eu.nk2.intercom.*
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.api.ProvideIntercom
import eu.nk2.intercom.utils.*
import eu.nk2.intercom.utils.asyncMap
import eu.nk2.intercom.utils.then
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.rabbitmq.OutboundMessage
import reactor.rabbitmq.RabbitFlux
import reactor.rabbitmq.SenderOptions
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

class IntercomProviderBeanPostProcessor(
    private val rabbitProperties: Pair<Mono<Connection>, String>,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor {
    private val log = LoggerFactory.getLogger(IntercomProviderBeanPostProcessor::class.java)

    private var channel: Mono<Channel>? = null

    @Order(Ordered.HIGHEST_PRECEDENCE) @EventListener fun init(event: ContextRefreshedEvent) {
        channel = rabbitProperties.first.asyncMap { it.createChannel() }
            .cache()
    }

    @Order(Ordered.LOWEST_PRECEDENCE) @EventListener fun dispose(event: ContextClosedEvent) {
        channel?.block()?.close()
    }

    private fun makeIntercomRequest(id: String, method: Method, args: Array<Any>): Mono<Any?> =
        (channel ?: error("Context was not initialized when making intercom request")).asyncMap {
            val queue = it.queueDeclare().queue
            val requestId = UUID.randomUUID().toString()
            val publisherId = id.hashCode()
            it.basicPublish(
                "",
                "${rabbitProperties.second}.$publisherId",
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
                    { _ -> sink.complete() }
                )
            }
            .publishOn(Schedulers.elastic())
        }.filter { (_, _, isRelated, _) -> isRelated }
            .asyncMap { (channel, tag, _, body) ->
                channel.basicCancel(tag)
                body
            }
            .map { Optional.ofNullable(intercomReturnBundleSerializer.deserialize<Any>(it)) }
            .filter { it.isPresent }
            .map { it.get() }
            .defaultIfEmpty(IntercomReturnBundle(error = ClientNoDataIntercomError, data = null))
            .next()
            .flatMap {
                if(it.error != null) Mono.error(when(it.error) {
                    is IntercomThrowableAwareError -> it.error.throwable
                    else -> IntercomException(it.error)
                })
                else Mono.justOrEmpty(it.data)
            }

    private fun mapProviderField(bean: Any, beanName: String, field: Field) {
        log.debug("Mapping $beanName's provider field ${field.name} to proxy")
        val id = field.getAnnotation(ProvideIntercom::class.java)?.id
            ?: error("id is required in annotation @ProvideIntercom")

        ReflectionUtils.makeAccessible(field)
        field.set(bean, Proxy.newProxyInstance(bean.javaClass.classLoader, arrayOf(field.type)) { _, method, args ->
            return@newProxyInstance makeIntercomRequest(id, method, args ?: arrayOf())
                .let {
                    when(method.returnType) {
                        Mono::class.java -> it
                        Flux::class.java -> it.flatMapMany { Flux.fromIterable(it as List<*>) }
                        else -> error("Impossible situation: return type of intercomRequest is not Publisher")
                    }
                }
        })
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any =
         bean.apply {
            ReflectionUtils.doWithFields(
                bean.javaClass,
                { field -> mapProviderField(bean, beanName, field) },
                { field -> field.isAnnotationPresent(ProvideIntercom::class.java) }
            )
        }
}
