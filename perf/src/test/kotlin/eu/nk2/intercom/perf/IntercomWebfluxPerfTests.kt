package eu.nk2.intercom.perf

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import kotlin.random.Random
import kotlin.system.measureTimeMillis


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntercomWebfluxPerfTests {
    private val logger = LoggerFactory.getLogger(IntercomWebfluxPerfTests::class.java)

    @LocalServerPort var randomServerPort = 0
    @Autowired private lateinit var webClient: WebClient

    private fun produceLoad() {
        val a = Random.nextInt()
        val b = Random.nextInt()

        assert(
            webClient.get()
                .uri {
                    it.scheme("http")
                        .host("localhost")
                        .port(randomServerPort)
                        .path("/perf")
                        .queryParam("a", a)
                        .queryParam("b", b)
                        .build()
                }
                .retrieve()
                .bodyToMono<String>()
                .block()!! == "${a+b}"
        ) {
            "WebFluxController must return valid answer"
        }
    }

    //
    // TODO: Sorry for this incomprehensible bullshit, i WILL refactor it one day
    //
    @Test
    fun testWebFluxCommunication() {
        logger.info("Warming up")
        (1..TEST_BENCHMARK_WARMUP_COUNT).forEach { produceLoad() }

        var time = 0.0
        logger.info("Performance testing")
        (1..(TEST_BENCHMARK_REPEAT_COUNT / TEST_BENCHMARK_REPEAT_BATCH_COUNT)).forEach {
            logger.info("Performance testing batch #$it")
            (1..TEST_BENCHMARK_REPEAT_BATCH_COUNT).forEach {
                time += measureTimeMillis { produceLoad() }
            }
            Thread.sleep(TEST_BENCHMARK_REPEAT_BATCH_WAIT_MS.toLong())
        }
        val result = time / TEST_BENCHMARK_REPEAT_COUNT

        logger.info("Result: $result")
    }
}