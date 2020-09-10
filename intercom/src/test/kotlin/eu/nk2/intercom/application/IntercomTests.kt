package eu.nk2.intercom.application

import eu.nk2.intercom.api.ProvideIntercom
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.ui.context.Theme
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CopyOnWriteArrayList

@SpringBootTest(classes = [IntercomApplication::class])
class IntercomTests {

    @ProvideIntercom(id = TEST_INTERFACE_INTERCOM_ID)
    private lateinit var testInterface: TestInterface

    @ProvideIntercom(id = TEST_SECOND_INTERFACE_INTERCOM_ID)
    private lateinit var secondTestInterface: TestInterface

    @ProvideIntercom(id = TEST_DELAYED_INTERFACE_INTERCOM_ID)
    private lateinit var testDelayedInterface: TestDelayedInterface

    @ProvideIntercom(id = TEST_SECOND_DELAYED_INTERFACE_INTERCOM_ID)
    private lateinit var secondTestDelayedInterface: TestDelayedInterface

    @Test
    fun intercomMethodWorks() {
        assert(testInterface.testA(1, 2).block() == "3") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomOverloadedMethodWorks() {
        assert(testInterface.testA(5).block() == "10") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomAnotherMethodWorks() {
        assert(testInterface.testB(4, "hello").block() == "hello4") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomOverloadedMethodDifferentTypeWorks() {
        assert(testInterface.testA("5").block() == "55") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomAnotherMethodOverloadedDifferentTypeWorks() {
        println(testInterface.testB("aaa", 4).blockFirst())
        assert(testInterface.testB("aaa", 4).blockFirst() == "4aaa") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomSecondInterfaceOfSameTypeWorks() {
        assert(secondTestInterface.testA(2, 3).block() == "6") { "SecondTestInterfaceImpl must return valid answer" }
        assert(secondTestInterface.testA( 3).block() == "9") { "SecondTestInterfaceImpl must return valid answer" }
        assert(secondTestInterface.testA("Ko").block() == "KoKoKoKo") { "SecondTestInterfaceImpl must return valid answer" }
        assert(secondTestInterface.testB(5, "Ko").block() == "Ko55Ko") { "SecondTestInterfaceImpl must return valid answer" }
        assert(secondTestInterface.testB("At", 3).blockFirst() == "3AtAt3") { "SecondTestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomBothTestDelayedInterfacesWorkFromDifferentThreads() {
        fun makeTestDelayedInterfaceThread() =
            AsyncTester {
                (1..TEST_DELAYED_BENCHMARK_REPEAT_COUNT).forEach {
                    assert(testDelayedInterface.test(TEST_DELAYED_BENCHMARK_DELAY_MS).block() == 1) { "TestDelayedInterfaceImpl must return valid answer" }
                }
            }

        fun makeTestSecondDelayedInterfaceThread() =
            AsyncTester {
                (1..TEST_DELAYED_BENCHMARK_REPEAT_COUNT).forEach {
                    assert(secondTestDelayedInterface.test(TEST_DELAYED_BENCHMARK_SECOND_DELAY_MS).block() == 2) { "TestSecondDelayedInterfaceImpl must return valid answer" }
                }
            }

        val threadList = CopyOnWriteArrayList<AsyncTester>()
        (1..TEST_DELAYED_BENCHMARK_THREAD_COUNT).forEach {
            threadList.add(
                makeTestDelayedInterfaceThread()
                    .also { it.start() }
            )
            threadList.add(
                makeTestSecondDelayedInterfaceThread()
                    .also { it.start() }
            )
        }

        threadList.forEach {
            it.test()
        }
    }
}
