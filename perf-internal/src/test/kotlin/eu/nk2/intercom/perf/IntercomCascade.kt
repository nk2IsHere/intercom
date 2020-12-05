package eu.nk2.intercom.perf

import eu.nk2.intercom.api.PublishIntercom
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kotlin.core.publisher.toMono

interface TestInterface {
    fun testA(a: Int, b: Int): Mono<String>
}

@Service
@PublishIntercom(id = TEST_INTERFACE_INTERCOM_ID)
class TestInterfaceImpl: TestInterface {

    override fun testA(a: Int, b: Int): Mono<String> =
        Mono.fromCallable { "${a+b}" }
            .subscribeOn(Schedulers.boundedElastic())
}