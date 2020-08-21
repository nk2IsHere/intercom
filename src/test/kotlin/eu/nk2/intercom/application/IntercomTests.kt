package eu.nk2.intercom.application

import eu.nk2.intercom.api.ProvideIntercom
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [IntercomApplication::class])
class IntercomTests {

    @field:ProvideIntercom(id = TEST_INTERFACE_INTERCOM_ID)
    private lateinit var testInterface: TestInterface

    @Test
    fun intercomMethodWorks() {
        assert(testInterface.testA(1, 2) == "3") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomOverloadedMethodWorks() {
        assert(testInterface.testA(5) == "10") { "TestInterfaceImpl must return valid answer" }
    }

    @Test
    fun intercomAnotherMethodWorks() {
        assert(testInterface.testB(4, "hello") == "hello4") { "TestInterfaceImpl must return valid answer" }
    }

}
