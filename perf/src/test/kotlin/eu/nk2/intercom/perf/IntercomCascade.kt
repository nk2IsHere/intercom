package eu.nk2.intercom.perf

import eu.nk2.intercom.api.PublishIntercom
import org.springframework.stereotype.Service

interface TestInterface {
    fun testA(a: Int, b: Int): String
}

@Service
@PublishIntercom(id = TEST_INTERFACE_INTERCOM_ID)
class TestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): String =
        "${a+b}"
}