package eu.nk2.intercom.boot

import eu.nk2.intercom.tcp.TcpServer
import eu.nk2.intercom.tcp.api.AbstractTcpServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(IntercomPropertiesConfiguration::class)
@ConditionalOnProperty(prefix = "intercom", name = ["serverMode", "host", "port"])
class IntercomAutoConfiguration {

    @Bean fun intercomAutoStarterApplicationListener(): IntercomAutoStarterApplicationListener =
        IntercomAutoStarterApplicationListener()

    @Bean
    @ConditionalOnProperty(prefix = "intercom", name = ["serverMode"], havingValue = "true", matchIfMissing = true)
    fun intercomTcpServer(
        @Autowired properties: IntercomPropertiesConfiguration
    ): AbstractTcpServer =
        TcpServer(
            port = properties.port ?: error("Intercom requires server port to be present in configuration")
        )

    @Bean
    @ConditionalOnProperty(prefix = "intercom", name = ["serverMode"], havingValue = "true", matchIfMissing = true)
    fun intercomPublisherBeanPostProcessor(
        @Autowired tcpServer: AbstractTcpServer
    ): IntercomPublisherBeanPostProcessor =
        IntercomPublisherBeanPostProcessor(
            tcpServer = tcpServer
        )

    @Bean
    fun intercomProviderBeanPostProcessor(
        @Autowired properties: IntercomPropertiesConfiguration
    ): IntercomProviderBeanPostProcessor =
        IntercomProviderBeanPostProcessor(
            host = properties.host ?: error("Intercom requires client host to be present in configuration"),
            port = properties.port ?: error("Intercom requires client port to be present in configuration")
        )
}