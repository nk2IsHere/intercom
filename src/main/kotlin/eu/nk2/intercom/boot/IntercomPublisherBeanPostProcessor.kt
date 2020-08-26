package eu.nk2.intercom.boot

import eu.nk2.intercom.ClassUtils
import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomMethodBundle
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.PublishIntercom
import eu.nk2.intercom.tcp.api.AbstractTcpConnection
import eu.nk2.intercom.tcp.api.AbstractTcpServer
import eu.nk2.intercom.tcp.api.TcpConnectionListener
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import org.springframework.util.SerializationUtils
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap


@Component class IntercomPublisherBeanPostProcessor(
    private val tcpServer: AbstractTcpServer
): BeanPostProcessor, PriorityOrdered {
    private val logger = LoggerFactory.getLogger(IntercomPublisherBeanPostProcessor::class.java)

    private val intercomPublisherAwareBeans = hashMapOf<String, Class<*>>()
    private val intercomPublishers: MutableMap<Int, Pair<Any, Map<Int, Method>>> = ConcurrentHashMap()

    private fun <T> AbstractTcpConnection.sendBundle(intercomReturnBundle: IntercomReturnBundle<T>): Unit =
        this.send(SerializationUtils.serialize(intercomReturnBundle)!!)
            .also { this.close() }

    @EventListener fun init(event: ContextRefreshedEvent) {
        tcpServer.addListener(object : TcpConnectionListener {
            override fun onConnected(connection: AbstractTcpConnection) {
                logger.debug("Connected intercom client: ${connection.address.canonicalHostName}")
            }

            override fun onDisconnected(connection: AbstractTcpConnection) {
                logger.debug("Disconnected intercom client: ${connection.address.canonicalHostName}")
            }

            override fun onMessageReceived(connection: AbstractTcpConnection, message: ByteArray) {
                try {
                    val bundle = SerializationUtils.deserialize(message) as? IntercomMethodBundle
                        ?: return connection.sendBundle(IntercomReturnBundle(
                            error = IntercomError.NO_DATA,
                            data = null
                        ))

                    logger.debug("Received from intercom client: ${connection.address.canonicalHostName}: ${bundle.publisherId}.${bundle.methodId}()")
                    val publisherDefinition = intercomPublishers[bundle.publisherId]
                        ?: return connection.sendBundle(IntercomReturnBundle(
                            error = IntercomError.BAD_PUBLISHER,
                            data = null
                        ))

                    val (publisher, method) = publisherDefinition.first to (
                        publisherDefinition.second[bundle.methodId]
                            ?: return connection.sendBundle(IntercomReturnBundle(
                                error = IntercomError.BAD_METHOD,
                                data = null
                            ))
                        )

                    if(method.parameterCount != bundle.parameters.size)
                        return connection.sendBundle(IntercomReturnBundle(
                            error = IntercomError.BAD_PARAMS,
                            data = null
                        ))
                    for((index, parameter) in method.parameters.withIndex())
                        if (ClassUtils.objectiveClass(parameter.type) != ClassUtils.objectiveClass(bundle.parameters[index].javaClass))
                            return connection.sendBundle(IntercomReturnBundle(
                                error = IntercomError.BAD_PARAMS,
                                data = null
                            ))

                    val output = method.invoke(publisher, *bundle.parameters)
                    return connection.sendBundle(IntercomReturnBundle(
                        error = null,
                        data = output
                    ))
                } catch (e: Exception) {
                    logger.debug("Error in handling intercom client", e)
                    return connection.sendBundle(IntercomReturnBundle(
                        error = when (e) {
                            is IllegalArgumentException -> IntercomError.BAD_DATA
                            is InvocationTargetException -> IntercomError.PROVIDER_ERROR
                            else -> IntercomError.INTERNAL_ERROR
                        },
                        data = null
                    ))
                }
            }
        })
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
                .map { it.name.hashCode() xor it.parameters.map { it.type.name }.hashCode() to it }
                .toMap())
            logger.debug("Mapped publisher $id to registry")
        }

        return super.postProcessAfterInitialization(bean, beanName)
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
