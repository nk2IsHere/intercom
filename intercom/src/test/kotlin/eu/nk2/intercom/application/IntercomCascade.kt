package eu.nk2.intercom.application

import eu.nk2.intercom.api.PublishIntercom
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

interface TestInterface {
    fun testA(a: Int, b: Int): String
    fun testA(a: Int): String
    fun testA(a: String): String
    fun testB(a: Int, b: String): String
    fun testB(a: String, b: Int): String
}

@Service
@PublishIntercom(id = TEST_INTERFACE_INTERCOM_ID)
class TestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): String =
        "${a+b}"

    override fun testA(a: Int): String =
        "${a+a}"

    override fun testA(a: String): String =
        "$a$a"

    override fun testB(a: Int, b: String): String =
        "$b$a"

    override fun testB(a: String, b: Int): String =
        "$b$a"
}

@Service
@PublishIntercom(id = TEST_SECOND_INTERFACE_INTERCOM_ID)
class SecondTestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): String =
        "${a*b}"

    override fun testA(a: Int): String =
        "${a*a}"

    override fun testA(a: String): String =
        "$a$a$a$a"

    override fun testB(a: Int, b: String): String =
        "$b$a$a$b"

    override fun testB(a: String, b: Int): String =
        "$b$a$a$b"
}

interface TestDelayedInterface {
    fun test(delay: Int): Int
}

@Service
@PublishIntercom(id = TEST_DELAYED_INTERFACE_INTERCOM_ID)
class TestDelayedInterfaceImpl: TestDelayedInterface {
    override fun test(delay: Int): Int {
        Thread.sleep(delay.toLong())
        return 1
    }

}

@Service
@PublishIntercom(id = TEST_SECOND_DELAYED_INTERFACE_INTERCOM_ID)
class TestSecondDelayedInterfaceImpl: TestDelayedInterface {
    override fun test(delay: Int): Int {
        Thread.sleep(delay.toLong())
        return 2
    }

}