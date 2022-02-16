package eu.nk2.intercom.boot

import eu.nk2.intercom.*
import eu.nk2.intercom.api.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.core.annotation.Order
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class IntercomPublisherBeanPostProcessor(
    private val publisherStreamFactory: IntercomPublisherStreamFactory
): BeanPostProcessor, PriorityOrdered, IntercomPublisherResponseFactory {

    private data class IntercomPublisherBeanMethodBundle(
        val publisherId: Int,
        val bean: Any,
        val methodIdToMethodMap: Map<Int, Method>
    )

    private val log = LoggerFactory.getLogger(IntercomPublisherBeanPostProcessor::class.java)

    private val intercomPublisherAwareBeans = hashMapOf<String, Class<*>>()

    private val intercomPublishers: MutableSet<IntercomPublisher> = ConcurrentHashMap.newKeySet()
    private val intercomPublishersToBeanMap: MutableMap<Int, IntercomPublisherBeanMethodBundle> = ConcurrentHashMap()

    @Order(Ordered.HIGHEST_PRECEDENCE)
    @EventListener
    fun init(event: ContextRefreshedEvent) {
        publisherStreamFactory.initializeStream(intercomPublishers, this)
    }

    @Order(Ordered.LOWEST_PRECEDENCE)
    @EventListener
    fun dispose(event: ContextClosedEvent) {
        publisherStreamFactory.disposeStream()
    }

    override fun buildResponse(publisherId: Int, methodId: Int, args: Array<*>): Mono<out Any?> {
        val publisher = intercomPublishersToBeanMap[publisherId]
            ?: return Mono.error(IntercomException(BadPublisherIntercomError))

        val method = publisher.methodIdToMethodMap[methodId]
            ?: return Mono.error(IntercomException(BadMethodIntercomError))

        if(method.parameterCount != args.size)
            return Mono.error(IntercomException(BadParamsIntercomError()))

        for((index, parameter) in method.parameters.withIndex())
            if (ClassUtils.objectiveClass(parameter.type) != ClassUtils.objectiveClass(args[index]?.javaClass))
                return Mono.error(IntercomException(BadParamsIntercomError(parameter.type, args[index]?.javaClass)))

        return try {
            when (method.returnType) {
                Mono::class.java -> (method.invoke(publisher.bean, *args) as Mono<out Any?>)
                Flux::class.java -> (method.invoke(publisher.bean, *args) as Flux<out Any?>).collectList()
                else -> Mono.error(IntercomException(BadMethodReturnTypeIntercomError))
            }
        } catch (e: Exception) {
            log.debug("Error in handling intercom client", e)
            Mono.error<Optional<Any>>(
                IntercomException(when (e) {
                    is IllegalArgumentException -> BadDataIntercomError
                    is InvocationTargetException -> ProviderIntercomError(e.targetException)
                    else -> InternalIntercomError(e)
                })
            )
        }

    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any =
        bean.apply {
            if (AnnotationUtils.getAnnotation(bean.javaClass, PublishIntercom::class.java) != null)
                intercomPublisherAwareBeans[beanName] = this.javaClass
        }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        intercomPublisherAwareBeans[beanName]?.let { beanClass ->
            val annotation = AnnotationUtils.getAnnotation(beanClass, PublishIntercom::class.java)
                ?: error("Annotation @PublishIntercom is required on class $beanClass")

            val publisherId = annotation.id.hashCode()

            val intercomClass = if(annotation.type != Unit::class) annotation.type.java else beanClass

            intercomPublishersToBeanMap[publisherId] = IntercomPublisherBeanMethodBundle(
                publisherId = publisherId,
                bean = bean,
                methodIdToMethodMap = intercomClass.methods
                    .asSequence()
                    .filter { method ->
                        INTERCOM_ALLOWED_GENERIC_METHOD_RETURN_TYPES
                            .any { it.isAssignableFrom(method.returnType) }
                    }
                    .map {
                        (it.name.hashCode() xor it.parameters.map { it.type.name }.hashCode()) to it
                    }
                    .toMap()
            )

            intercomPublishers.add(IntercomPublisher(
                publisherId = publisherId,
                methods = intercomPublishersToBeanMap[publisherId]
                    ?.methodIdToMethodMap
                    ?.asSequence()
                    ?.map { IntercomPublisherMethod(it.key) }
                    ?.toSet()
                    ?: setOf()
            ))

            log.debug("Mapped publisher ${annotation.id} to registry")
        }

        return super.postProcessAfterInitialization(bean, beanName)
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
