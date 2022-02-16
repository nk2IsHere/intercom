package eu.nk2.intercom.boot

import eu.nk2.intercom.api.IntercomProviderResolutionRegistry
import eu.nk2.intercom.api.IntercomProviderStreamFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionRegistry
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor
import org.springframework.beans.factory.support.RootBeanDefinition
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.lang.reflect.Proxy
import java.util.function.Supplier

class IntercomProviderBeanDefinitionRegistryPostProcessor(
    private val providerResolutionRegistry: IntercomProviderResolutionRegistry<*>,
    private val providerStreamFactory: IntercomProviderStreamFactory
): BeanDefinitionRegistryPostProcessor {

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) = Unit

    override fun postProcessBeanDefinitionRegistry(registry: BeanDefinitionRegistry) {
        providerResolutionRegistry.get()
            .forEach {
                registry.registerBeanDefinition(
                    it.id,
                    RootBeanDefinition(it.type as Class<Any?>, BeanDefinition.SCOPE_SINGLETON, Supplier {
                        //TODO: duplicate at deprecated ProviderBPP
                        Proxy.newProxyInstance(
                            registry.javaClass.classLoader,
                            arrayOf(it.type)
                        ) { _, method, args ->
                            return@newProxyInstance providerStreamFactory
                                .buildRequest(
                                    publisherRegistryId = it.id,
                                    publisherId = it.id.hashCode(),
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
                        }
                    })
                )
            }
    }
}
