package eu.nk2.intercom.application

import eu.nk2.intercom.api.ProvideIntercom
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [IntercomApplication::class])
class IntercomTests {

    @ProvideIntercom(id = TEST_INTERFACE_INTERCOM_ID)
    private lateinit var testInterface: TestInterface

    @Test
    fun intercomMethodWorks() {
        (1..TEST_BENCHMARK_REPEAT_COUNT).forEach {
            assert(testInterface.testA(1, 2) == "3") { "TestInterfaceImpl must return valid answer" }
        }
    }

    @Test
    fun intercomOverloadedMethodWorks() {
        assert(testInterface.testA(5) == "10") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomAnotherMethodWorks() {
        assert(testInterface.testB(4, "hello") == "hello4") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomOverloadedMethodDifferentTypeWorks() {
        assert(testInterface.testA("5") == "55") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomAnotherMethodOverloadedDifferentTypeWorks() {
        assert(testInterface.testB("aaa", 4) == "4aaa") { "TestInterfaceImpl must return valid answer" }
    }
}
