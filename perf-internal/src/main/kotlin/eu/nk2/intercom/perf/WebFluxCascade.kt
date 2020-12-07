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
import org.springframework.web.reactive.function.server.body
import org.springframework.web.reactive.function.server.bodyToMono
import org.springframework.web.reactive.function.server.router
import reactor.netty.http.client.HttpClient
import reactor.netty.tcp.TcpClient

data class WebFluxRequest(
    var id: String,
    var args: Array<Any>
) {
    constructor(): this("", arrayOf())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebFluxRequest

        if (id != other.id) return false
        if (!args.contentEquals(other.args)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + args.contentHashCode()
        return result
    }
}

data class WebFluxResponse<T>(
    var error: WebFluxResponseError?,
    var data: T?
) {
   constructor(): this(null, null)

    enum class WebFluxResponseError {
        NOT_FOUND
    }
}

const val WEB_FLUX_ID_ADD = "WEB_FLUX_ID_ADD"

@Service class WebFluxController {

    @Bean
    fun appController() = router {
        POST("/perf") {
            ok().contentType(MediaType.APPLICATION_JSON)
                .body(
                    it.bodyToMono(WebFluxRequest::class.java)
                        .map { when(it.id) {
                            WEB_FLUX_ID_ADD -> WebFluxResponse(error = null, data = "${(it.args[0] as Int) + (it.args[1] as Int)}")
                            else -> WebFluxResponse(error = WebFluxResponse.WebFluxResponseError.NOT_FOUND, data = null)
                        } }
                )
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
