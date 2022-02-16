package eu.nk2.intercom.boot

import eu.nk2.intercom.api.IntercomProviderStreamFactory
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.context.EnvironmentAware
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.env.EnumerablePropertySource
import org.springframework.core.env.Environment


class IntercomProviderBeanDefinitionRegistryPostProcessor(
    private val providerStreamFactory: IntercomProviderStreamFactory
): BeanDefinitionRegistryPostProcessor, EnvironmentAware {

    private lateinit var environment: Environment

    override fun setEnvironment(environment: Environment) {
        this.environment = environment
    }

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) = Unit

    private fun allPropertiesByPrefix(prefix: String): MutableMap<String, Any?> {
        val properties: MutableMap<String, Any?> = HashMap()
        if (environment is ConfigurableEnvironment) {
            for (propertySource in (environment as ConfigurableEnvironment).propertySources) {
                if (propertySource is EnumerablePropertySource<*>) {
                    for (key in propertySource.propertyNames) {
                        if (key.startsWith(prefix)) {
                            properties[key] = propertySource.getProperty(key)
                        }
                    }
                }
            }
        }

        return properties
    }

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        println(allPropertiesByPrefix("intercom.client.routes"))

//        providerResolutionRegistry.get()
//            .forEach {
//                registry.registerBeanDefinition(
//                    it.id,
//                    RootBeanDefinition(it.type as Class<Any?>, BeanDefinition.SCOPE_SINGLETON, Supplier {
//                        //TODO: duplicate at deprecated ProviderBPP
//                        Proxy.newProxyInstance(
//                            registry.javaClass.classLoader,
//                            arrayOf(it.type)
//                        ) { _, method, args ->
//                            return@newProxyInstance providerStreamFactory
//                                .buildRequest(
//                                    publisherRegistryId = it.id,
//                                    publisherId = it.id.hashCode(),
//                                    methodId = method.name.hashCode() xor method.parameters.map { it.type.name }.hashCode(),
//                                    args = args ?: arrayOf()
//                                )
//                                .let {
//                                    when(method.returnType) {
//                                        Mono::class.java -> it
//                                        Flux::class.java -> it.flatMapMany { Flux.fromIterable(it as List<*>) }
//                                        else -> error("Impossible situation: return type of intercomRequest is not Publisher")
//                                    }
//                                }
//                        }
//                    })
//                )
//            }
    }
}
