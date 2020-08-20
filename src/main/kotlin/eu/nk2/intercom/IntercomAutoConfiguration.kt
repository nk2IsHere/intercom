package eu.nk2.intercom

import eu.nk2.intercom.tcp.TcpServer
import eu.nk2.intercom.tcp.api.AbstractTcpServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(IntercomPropertiesConfiguration::class)
@ConditionalOnProperty(prefix = "intercom", name = ["port"])
class IntercomAutoConfiguration {

    @Bean fun intercomAutoStarterApplicationListener(): IntercomAutoStarterApplicationListener =
        IntercomAutoStarterApplicationListener()

    @Bean fun intercomTcpServer(
        @Autowired properties: IntercomPropertiesConfiguration
    ): AbstractTcpServer =
        TcpServer(
            port = properties.port ?: error("Intercom requires TCP port to be present in configuration")
        )
}