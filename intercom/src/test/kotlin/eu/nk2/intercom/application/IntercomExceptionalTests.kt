package eu.nk2.intercom.application

import eu.nk2.intercom.api.ProvideIntercom
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import reactor.kotlin.core.publisher.toMono
import java.util.concurrent.TimeoutException

@SpringBootTest(classes = [IntercomApplication::class])
class IntercomExceptionalTests {

    @ProvideIntercom(TEST_EXCEPTIONAL_INTERFACE_ID)
    private lateinit var testExceptionalInterface: TestExceptionalInterface

    @ProvideIntercom(TEST_TIMEOUT_EXCEPTIONAL_INTERFACE_ID)
    private lateinit var testTimeoutExceptionalInterface: TestTimeoutExceptionalInterface

    @Test
    fun testEmptyMono() {
        assert(
            testExceptionalInterface.emptyResultMono()
                .switchIfEmpty("empty".toMono()).block() == "empty"
        ) { "Mono is not empty" }
    }

    @Test
    fun testEmptyFlux() {
        assert(
            testExceptionalInterface.emptyResultFlux()
                .hasElements()
                .block()!!
                .not()
        ) { "Flux is not empty" }
    }

    @Test
    fun testErrorMono() {
        assert(
            testExceptionalInterface.errorResultMono()
                .onErrorResume(IllegalStateException::class.java) { "error".toMono() }
                .onErrorReturn("anotherError")
                .block() == "error"
        ) { "Mono was not returning IllegalStateException" }
    }

    @Test
    fun testErrorFlux() {
        assert(
            testExceptionalInterface.errorResultFlux()
                .onErrorResume(IllegalStateException::class.java) { "error".toMono() }
                .onErrorReturn("anotherError")
                .blockFirst() == "error"
        ) { "Flux was not returning IllegalStateException" }
    }

    @Test
    fun testNoResultMono() {
        assert(
            testTimeoutExceptionalInterface.noResultMono()
                .onErrorResume(TimeoutException::class.java) { "error".toMono() }
                .onErrorReturn("anotherError")
                .block() == "error"
        ) { "Mono was not returning TimeoutException" }
    }

    @Test
    fun testNoResultFlux() {
        assert(
            testTimeoutExceptionalInterface.noResultFlux()
                .onErrorResume(TimeoutException::class.java) { "error".toMono() }
                .onErrorReturn("anotherError")
                .blockFirst() == "error"
        ) { "Flux was not returning TimeoutException" }
    }
}
