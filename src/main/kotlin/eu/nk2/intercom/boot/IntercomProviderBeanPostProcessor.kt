package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomException
import eu.nk2.intercom.IntercomMethodBundle
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.ProvideIntercom
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import org.springframework.util.SerializationUtils
import java.lang.reflect.Field
import java.lang.reflect.Proxy
import java.net.Socket
import kotlin.reflect.full.declaredMembers

@Component
class IntercomProviderBeanPostProcessor(
    private val host: String,
    private val port: Int
): BeanPostProcessor {
    private val logger = LoggerFactory.getLogger(IntercomProviderBeanPostProcessor::class.java)

    fun error(msg: String, error: IntercomError) {
        throw IntercomException(msg, error)
    }

    fun mapProviderField(bean: Any, beanName: String, field: Field) {
        logger.debug("Mapping $beanName's provider field to proxy")
        val id = field.getAnnotation(ProvideIntercom::class.java)?.id
            ?: error("id is required in annotation @ProvideIntercom")

        ReflectionUtils.makeAccessible(field);
        field.set(bean, Proxy.newProxyInstance(bean.javaClass.classLoader, arrayOf(field.type)) { _, method, args ->
            val bundle = IntercomMethodBundle(
                publisherId = id.hashCode(),
                methodId = method.name.hashCode() xor method.parameters.map { it.type.packageName }.hashCode(),
                parameters = args
            )

            val socket = Socket(host, port)
            socket.getOutputStream().write(SerializationUtils.serialize(bundle)!!)

            val data = SerializationUtils.deserialize(socket.getInputStream().readAllBytes())
                as? IntercomReturnBundle<Any>
                ?: error("Received unexpected data type")

            if(data.error != null)
                when(data.error) {
                    IntercomError.NO_DATA -> error("Server received no data", data.error)
                    IntercomError.BAD_DATA -> error("Server received bad data", data.error)
                    IntercomError.BAD_PUBLISHER -> error("Server received bad publisher - it cannot be found", data.error)
                    IntercomError.BAD_METHOD -> error("Server received bad method - it cannot be found", data.error)
                    IntercomError.BAD_PARAMS -> error("Server received bad parameters - args count or types mismatch", data.error)
                    IntercomError.PROVIDER_ERROR -> error("Server produced provider error - check logs", data.error)
                    IntercomError.INTERNAL_ERROR -> error("Server received internal error - check logs and mentally punch the author", data.error)
                }

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