package eu.nk2.intercom.application

import eu.nk2.intercom.api.ProvideIntercom
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(classes = [IntercomApplication::class])
class IntercomTests {

    @ProvideIntercom(id = TEST_INTERFACE_INTERCOM_ID)
    private lateinit var testInterface: TestInterface

    @Test
    fun intercomBlockingCommunication() {
        assert(testInterface.testA(1, 2) == "3") { "TestInterfaceImpl must return valid answer" }
    }

}
