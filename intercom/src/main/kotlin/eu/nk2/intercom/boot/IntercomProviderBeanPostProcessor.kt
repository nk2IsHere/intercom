package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomException
import eu.nk2.intercom.IntercomMethodBundle
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.api.ProvideIntercom
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.netty.Connection
import reactor.netty.tcp.TcpClient
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.Duration
import java.util.*

@Component
class IntercomProviderBeanPostProcessor(
    private val tcpClient: TcpClient,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor {
    private val logger = LoggerFactory.getLogger(IntercomProviderBeanPostProcessor::class.java)

    fun makeTcpConnectionMono(connection: Connection, id: String, method: Method, args: Array<Any>): Mono<Any?> {
        connection.outbound()
            .sendByteArray(Mono.fromCallable { intercomMethodBundleSerializer.serialize(IntercomMethodBundle(
                publisherId = id.hashCode(),
                methodId = method.name.hashCode() xor method.parameters.map { it.type.name }.hashCode(),
                parameters = args
            )) })
            .then()
            .subscribe()

        return connection.inbound().receive()
            .asByteArray()
            .map { Optional.ofNullable(intercomReturnBundleSerializer.deserialize<Any>(it)) }
            .filter { it.isPresent }
            .map { it.get() }
            .defaultIfEmpty(IntercomReturnBundle(error = IntercomError.INTERNAL_ERROR, data = null))
            .doOnNext { if(it.error != null) throw IntercomException(it.error) }
            .flatMap { it.data?.toMono() ?: Mono.empty() }
            .next()
            .doOnNext { connection.disposeNow(Duration.ZERO) }
    }

    fun mapProviderField(bean: Any, beanName: String, field: Field) {
        logger.debug("Mapping $beanName's provider field to proxy")
        val id = field.getAnnotation(ProvideIntercom::class.java)?.id
            ?: error("id is required in annotation @ProvideIntercom")

        ReflectionUtils.makeAccessible(field)
        field.set(bean, Proxy.newProxyInstance(bean.javaClass.classLoader, arrayOf(field.type)) { _, method, args ->
            return@newProxyInstance tcpClient.connect()
                .flatMap { makeTcpConnectionMono(it, id, method, args) }
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