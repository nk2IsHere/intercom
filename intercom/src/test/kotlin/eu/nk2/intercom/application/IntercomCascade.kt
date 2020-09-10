package eu.nk2.intercom.application

import eu.nk2.intercom.api.PublishIntercom
import org.junit.jupiter.api.Test
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

interface TestInterface {
    fun testA(a: Int, b: Int): Mono<String>
    fun testA(a: Int): Mono<String>
    fun testA(a: String): Mono<String>
    fun testB(a: Int, b: String): Mono<String>
    fun testB(a: String, b: Int): Flux<String>
}

@Service
@PublishIntercom(id = TEST_INTERFACE_INTERCOM_ID)
class TestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): Mono<String> =
        "${a+b}".toMono()

    override fun testA(a: Int): Mono<String> =
        "${a+a}".toMono()

    override fun testA(a: String): Mono<String> =
        "$a$a".toMono()

    override fun testB(a: Int, b: String): Mono<String> =
        "$b$a".toMono()

    override fun testB(a: String, b: Int): Flux<String> =
        Flux.fromArray(arrayOf("$b$a"))
}

@Service
@PublishIntercom(id = TEST_SECOND_INTERFACE_INTERCOM_ID)
class SecondTestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): Mono<String> =
        "${a*b}".toMono()

    override fun testA(a: Int): Mono<String> =
        "${a*a}".toMono()

    override fun testA(a: String): Mono<String> =
        "$a$a$a$a".toMono()

    override fun testB(a: Int, b: String): Mono<String> =
        "$b$a$a$b".toMono()

    override fun testB(a: String, b: Int): Flux<String> =
        Flux.fromArray(arrayOf("$b$a$a$b"))
}

interface TestDelayedInterface {
    fun test(delay: Int): Mono<Int>
}

@Service
@PublishIntercom(id = TEST_DELAYED_INTERFACE_INTERCOM_ID)
class TestDelayedInterfaceImpl: TestDelayedInterface {
    override fun test(delay: Int): Mono<Int> = Mono.create { supplier ->
        Thread.sleep(delay.toLong())
        supplier.success(1)
    }

}

@Service
@PublishIntercom(id = TEST_SECOND_DELAYED_INTERFACE_INTERCOM_ID)
class TestSecondDelayedInterfaceImpl: TestDelayedInterface {
    override fun test(delay: Int): Mono<Int> = Mono.create { supplier ->
        Thread.sleep(delay.toLong())
        supplier.success(2)
    }

}