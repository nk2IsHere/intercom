package eu.nk2.intercom.boot

import eu.nk2.intercom.api.IntercomProviderStreamFactory
import eu.nk2.intercom.api.ProvideIntercom
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
import java.lang.reflect.Field
import java.lang.reflect.Proxy

class IntercomProviderBeanPostProcessor(
    private val providerStreamFactory: IntercomProviderStreamFactory
): BeanPostProcessor {
    private val log = LoggerFactory.getLogger(IntercomProviderBeanPostProcessor::class.java)

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    fun init(event: ContextRefreshedEvent) {
        providerStreamFactory.initialize()
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener
    fun dispose(event: ContextClosedEvent) {
        providerStreamFactory.dispose()
    }

    private fun mapProviderField(bean: Any, beanName: String, field: Field) {
        log.debug("Mapping $beanName's provider field ${field.name} to proxy")
        val id = field.getAnnotation(ProvideIntercom::class.java)?.id
            ?: error("id is required in annotation @ProvideIntercom")

        ReflectionUtils.makeAccessible(field)
        field.set(bean, Proxy.newProxyInstance(bean.javaClass.classLoader, arrayOf(field.type)) { _, method, args ->
            return@newProxyInstance providerStreamFactory
                .buildRequest(
                    publisherRegistryId = id,
                    publisherId = id.hashCode(),
                    methodId = method.name.hashCode() xor method.parameters.map { it.type.name }.hashCode(),
                    args = args ?: arrayOf()
                )
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
