package eu.nk2.intercom.perf

import org.springframework.context.annotation.Bean
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.server.router

@Service
class WebFluxController {

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