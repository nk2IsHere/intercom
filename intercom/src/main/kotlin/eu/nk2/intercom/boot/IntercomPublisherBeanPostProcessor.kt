package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomException
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.api.PublishIntercom
import eu.nk2.intercom.utils.orNull
import eu.nk2.intercom.utils.wrapOptional
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.netty.DisposableServer
import reactor.netty.tcp.TcpServer
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@Component class IntercomPublisherBeanPostProcessor(
    private val tcpServer: TcpServer,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor, PriorityOrdered {
    private val logger = LoggerFactory.getLogger(IntercomPublisherBeanPostProcessor::class.java)

    private val intercomPublisherAwareBeans = hashMapOf<String, Class<*>>()
    private val intercomPublishers: MutableMap<Int, Pair<Any, Map<Int, Method>>> = ConcurrentHashMap()

    private lateinit var tcpServerDisposable: DisposableServer

    @EventListener fun init(event: ContextRefreshedEvent) {
        tcpServerDisposable = tcpServer
            .doOnConnection {
                logger.debug("Connected intercom client: ${it.address()}")
            }
            .handle { input, output ->
                output.sendByteArray(
                    input.receive().asByteArray()
                        .map { Optional.ofNullable(intercomMethodBundleSerializer.deserialize(it)) }
                        .filter { it.isPresent }
                        .map { it.get() }
                        .doOnNext { logger.debug("Received from intercom client ${it.publisherId}.${it.methodId}()") }
                        .flatMap {
                            val publisherDefinition = intercomPublishers[it.publisherId]
                                ?: return@flatMap Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_PUBLISHER))

                            val (publisher, method) = publisherDefinition.first to (publisherDefinition.second[it.methodId]
                                ?: return@flatMap Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_METHOD)))

                            if(method.parameterCount != it.parameters.size)
                                return@flatMap Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_PARAMS))

                            for((index, parameter) in method.parameters.withIndex())
                                if (ClassUtils.objectiveClass(parameter.type) != ClassUtils.objectiveClass(it.parameters[index].javaClass))
                                    return@flatMap Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_PARAMS))

                            return@flatMap try {
                                when (method.returnType) {
                                    Mono::class.java -> (method.invoke(publisher, *it.parameters) as Mono<*>)
                                    Flux::class.java -> (method.invoke(publisher, *it.parameters) as Flux<*>).collectList()
                                    else -> Mono.error(IntercomException(IntercomError.INTERNAL_ERROR))
                                }.wrapOptional()
                            } catch (e: Exception) {
                                logger.debug("Error in handling intercom client", e)
                                Mono.error<Optional<Any>>(IntercomException(when (e) {
                                    is IllegalArgumentException -> IntercomError.BAD_DATA
                                    is InvocationTargetException -> IntercomError.PROVIDER_ERROR
                                    else -> IntercomError.INTERNAL_ERROR
                                }))
                            }
                        }
                        .map { IntercomReturnBundle<Any?>(error = null, data = it.orNull()) }
                        .onErrorResume(IntercomException::class.java) { Mono.just(IntercomReturnBundle<Any?>(error = it.error, data = null)) }
                        .defaultIfEmpty(IntercomReturnBundle(error = IntercomError.NO_DATA, data = null))
                        .map { intercomReturnBundleSerializer.serialize(it) }
                )
            }
            .bindNow()
    }

    @EventListener fun dispose(event: ContextClosedEvent) {
        tcpServerDisposable.disposeNow()
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
            logger.debug("Mapped publisher $id to registry")
        }

        return super.postProcessAfterInitialization(bean, beanName)
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
