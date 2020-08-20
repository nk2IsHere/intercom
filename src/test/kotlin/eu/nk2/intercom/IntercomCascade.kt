package eu.nk2.intercom

import eu.nk2.intercom.api.ProvideIntercom
import eu.nk2.intercom.api.PublishIntercom
import org.springframework.stereotype.Component

interface TestInterface {
    fun testA(a: Int, b: Int): String
}

@Component
@PublishIntercom(id = TEST_INTERFACE_INTERCOM_ID)
class TestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): String =
        "${a+b}"
}