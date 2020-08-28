package eu.nk2.intercom.boot

import eu.nk2.intercom.DefaultIntercomMethodBundleSerializer
import eu.nk2.intercom.DefaultIntercomReturnBundleSerializer
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.tcp.TcpServer
import eu.nk2.intercom.tcp.api.AbstractTcpServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
@EnableConfigurationProperties(IntercomPropertiesConfiguration::class)
@ConditionalOnProperty(prefix = "intercom", name = ["serverMode", "host", "port"])
class IntercomAutoConfiguration {

    @Bean fun intercomAutoStarterApplicationListener(): IntercomAutoStarterApplicationListener =
        IntercomAutoStarterApplicationListener()

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(prefix = "intercom", name = ["serverMode"], havingValue = "true", matchIfMissing = true)
    fun intercomTcpServer(
        @Autowired properties: IntercomPropertiesConfiguration
    ): AbstractTcpServer =
        TcpServer(
            port = properties.port ?: error("Intercom requires server port to be present in configuration")
        )

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(prefix = "intercom", name = ["serverMode"], havingValue = "true", matchIfMissing = true)
    fun intercomPublisherBeanPostProcessor(
        @Autowired tcpServer: AbstractTcpServer,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomPublisherBeanPostProcessor =
        IntercomPublisherBeanPostProcessor(
            tcpServer = tcpServer,
            intercomMethodBundleSerializer = intercomMethodBundleSerializer,
            intercomReturnBundleSerializer = intercomReturnBundleSerializer
        )

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun intercomProviderBeanPostProcessor(
        @Autowired properties: IntercomPropertiesConfiguration,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomProviderBeanPostProcessor =
        IntercomProviderBeanPostProcessor(
            host = properties.host ?: error("Intercom requires client host to be present in configuration"),
            port = properties.port ?: error("Intercom requires client port to be present in configuration"),
            socketErrorTolerance = properties.socketErrorTolerance ?: false,
            socketErrorMaxAttempts = properties.socketErrorMaxAttempts ?: 0,
            intercomMethodBundleSerializer = intercomMethodBundleSerializer,
            intercomReturnBundleSerializer = intercomReturnBundleSerializer
        )

    @Bean
    @ConditionalOnMissingBean(IntercomMethodBundleSerializer::class)
    fun intercomMethodBundleSerializer(
    ): IntercomMethodBundleSerializer =
        DefaultIntercomMethodBundleSerializer(
        )

    @Bean
    @ConditionalOnMissingBean(IntercomReturnBundleSerializer::class)
    fun intercomReturnBundleSerializer(
    ): IntercomReturnBundleSerializer =
        DefaultIntercomReturnBundleSerializer(
        )
}