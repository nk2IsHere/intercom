package eu.nk2.intercom.perf

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.server.router
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient

@Service class WebFluxController {

    @Bean
    fun appController() = router {
        GET("/perf") {
            if(!it.queryParam("a").isPresent || !it.queryParam("b").isPresent)
                return@GET notFound().build()

            val a = it.queryParam("a").get().toInt()
            val b = it.queryParam("b").get().toInt()

            ok().contentType(MediaType.TEXT_PLAIN)
                .bodyValue((a+b).toString())
        }
    }
}

@Configuration class WebFluxClient {
    private fun tcpClient() = TcpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
        .doOnConnected { connection ->
            connection.addHandlerLast(ReadTimeoutHandler(5))
                .addHandlerLast(WriteTimeoutHandler(5))
        }

    @Bean fun httpClient() = WebClient.builder()
        .clientConnector(ReactorClientHttpConnector(HttpClient.from(tcpClient())))
        .build()
}