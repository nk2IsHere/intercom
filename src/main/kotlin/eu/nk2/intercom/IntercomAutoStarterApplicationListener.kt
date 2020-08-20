package eu.nk2.intercom

import eu.nk2.intercom.IntercomPropertiesConfiguration
import eu.nk2.intercom.tcp.api.AbstractTcpServer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.stereotype.Component

@Component class IntercomAutoStarterApplicationListener : ApplicationListener<ContextRefreshedEvent> {
    @Autowired private lateinit var server: AbstractTcpServer

    override fun onApplicationEvent(event: ContextRefreshedEvent) {
        server.start()
    }
}