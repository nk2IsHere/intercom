package eu.nk2.intercom.api

import reactor.core.publisher.Mono
import java.util.*

interface IntercomPublisherResponseFactory {
    fun buildResponse(publisherId: Int, methodId: Int, args: Array<*>): Mono<out Any?>
}
