package eu.nk2.intercom.utils

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

internal fun <T, R> Mono<T>.asyncMap(mapper: (T) -> R): Mono<R> =
    this.flatMap {
        Mono.fromCallable {
            mapper(it)
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

internal fun <T, R> Flux<T>.asyncMap(mapper: (T) -> R): Flux<R> =
    this.flatMap {
        Mono.fromCallable {
            mapper(it)
        }
            .subscribeOn(Schedulers.boundedElastic())
    }
