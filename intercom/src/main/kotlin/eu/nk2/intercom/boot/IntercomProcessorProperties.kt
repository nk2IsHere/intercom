package eu.nk2.intercom.boot

import com.rabbitmq.client.Connection
import reactor.core.publisher.Mono

data class IntercomProcessorProperties(
    val connection: Mono<Connection>,
    val queuePrefix: String,
    val timeoutMillis: Long
)