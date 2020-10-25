package eu.nk2.intercom.utils

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import java.util.*

internal fun <T, R> Mono<T>.asyncMap(mapper: (T) -> R): Mono<R> =
    this.flatMap {
        Mono.fromCallable { mapper(it) }
            .subscribeOn(Schedulers.boundedElastic())
    }

internal fun <T, R> Flux<T>.asyncMap(mapper: (T) -> R): Flux<R> =
    this.flatMap {
        Mono.fromCallable { mapper(it) }
            .subscribeOn(Schedulers.boundedElastic())
    }

internal fun <T> Mono<T>.wrapOptional(): Mono<Optional<T>> =
    this.map { Optional.of(it) }
        .defaultIfEmpty(Optional.empty())

internal fun <T> Optional<T>.orNull(): T? =
    if(this.isPresent) this.get()
    else null

fun <T, R> Flux<T>.firstMapWith(other: R) =
    this.map { other to it }

fun <T, R> Flux<T>.firstMapWith(other: Function1<T, R>) =
    this.asyncMap { other(it) to it }

fun <T, R, U> Flux<Pair<T, R>>.firstMapWithPair(other: U) =
    this.map { (first, second) -> Triple(other, first, second) }

fun <T, R> Mono<T>.firstMapWith(other: R) =
    this.map { other to it }

fun <T, R, U> Mono<T>.firstMapWith(other1: R, other2: U) =
    this.map { Triple(other1, other2, it) }

fun <T, R> Mono<T>.firstMapWith(other: Function1<T, R>) =
    this.asyncMap { other(it) to it }

fun <T, R, U> Mono<Pair<T, R>>.firstMapWithPair(other: U) =
    this.map { (first, second) -> Triple(other, first, second) }
