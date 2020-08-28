package eu.nk2.intercom.perf

import eu.nk2.intercom.api.ProvideIntercom
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono


@SpringBootTest(classes = [PerfApplication::class])
class IntercomPerfTests {

    @ProvideIntercom(id = TEST_INTERFACE_INTERCOM_ID)
    private lateinit var testInterface: TestInterface

    @Autowired private lateinit var webClient: WebClient

    @Test
    fun testWebFluxCommunication() {
        assert(
            webClient.get()
                .uri {
                    it.scheme("http")
                        .host("localhost")
                        .port(8080)
                        .path("/perf")
                        .queryParam("a", 1)
                        .queryParam("b", 2)
                        .build()
                }
                .retrieve()
                .bodyToMono<String>()
                .block()!! == "3"
        ) {
            "WebFluxController must return valid answer"
        }
    }

    @Test
    fun testIntercomCommunication() {
        assert(testInterface.testA(1, 2) == "3") { "TestInterfaceImpl must return valid answer" }
    }
}