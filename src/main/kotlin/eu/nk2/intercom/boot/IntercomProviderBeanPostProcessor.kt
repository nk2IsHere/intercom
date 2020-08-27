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
import org.springframework.util.SerializationUtils
import java.lang.reflect.Field
import java.lang.reflect.Proxy
import java.net.Socket

@Component
class IntercomProviderBeanPostProcessor(
    private val host: String,
    private val port: Int,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor {
    private val logger = LoggerFactory.getLogger(IntercomProviderBeanPostProcessor::class.java)

    fun error(error: IntercomError) {
        throw IntercomException(error.message, error)
    }

    fun mapProviderField(bean: Any, beanName: String, field: Field) {
        logger.debug("Mapping $beanName's provider field to proxy")
        val id = field.getAnnotation(ProvideIntercom::class.java)?.id
            ?: error("id is required in annotation @ProvideIntercom")

        ReflectionUtils.makeAccessible(field)
        field.set(bean, Proxy.newProxyInstance(bean.javaClass.classLoader, arrayOf(field.type)) { _, method, args ->
            val bundle = IntercomMethodBundle(
                publisherId = id.hashCode(),
                methodId = method.name.hashCode() xor method.parameters.map { it.type.name }.hashCode(),
                parameters = args
            )

            val socket = Socket(host, port)
            socket.getOutputStream().write(intercomMethodBundleSerializer.serialize(bundle))

            val data = intercomReturnBundleSerializer.deserialize<Any>(socket.getInputStream().readBytes())
                ?: error("Received unexpected data type")

            socket.close()

            if(data.error != null)
               error(data.error)

            data.data
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