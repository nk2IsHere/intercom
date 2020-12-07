package eu.nk2.intercom.perf

import eu.nk2.intercom.api.ProvideIntercom
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.SpringBootTest
import kotlin.random.Random
import kotlin.system.measureTimeMillis

@SpringBootTest
class IntercomPerfTests {
    private val logger = LoggerFactory.getLogger(IntercomPerfTests::class.java)

    @ProvideIntercom(id = TEST_INTERFACE_INTERCOM_ID)
    private lateinit var testInterface: TestInterface

    private fun produceLoad() {
        val a = Random.nextInt()
        val b = Random.nextInt()

        assert(testInterface.testA(a, b).block() == "${a+b}") { "TestInterfaceImpl must return valid answer" }
    }

    //
    // TODO: Sorry for this incomprehensible bullshit, i WILL refactor it one day
    //
    @Test
    fun testIntercomCommunication() {
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
