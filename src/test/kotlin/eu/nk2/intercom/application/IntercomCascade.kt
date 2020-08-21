package eu.nk2.intercom.application

import eu.nk2.intercom.api.PublishIntercom
import org.springframework.stereotype.Component

interface TestInterface {
    fun testA(a: Int, b: Int): String
    fun testA(a: Int): String
    fun testB(a: Int, b: String): String
}

@Component
@PublishIntercom(id = TEST_INTERFACE_INTERCOM_ID)
class TestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): String =
        "${a+b}"

    override fun testA(a: Int): String =
        "${a+a}"

    override fun testB(a: Int, b: String): String =
        "$b$a"
}