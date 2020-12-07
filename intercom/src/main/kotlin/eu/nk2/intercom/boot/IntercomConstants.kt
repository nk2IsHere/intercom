package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomStarterMode
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

const val INTERCOM_RABBIT_CONNECTION_BEAN_ID = "intercomRabbitConnection"

internal val INTERCOM_ALLOWED_GENERIC_METHOD_RETURN_TYPES = arrayOf(Mono::class.java, Flux::class.java)

internal const val INTERCOM_PROPERTIES_PREFIX = "intercom"

val INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES = arrayOf("starter_mode", "starter-mode", "starterMode")
    .map { "$INTERCOM_PROPERTIES_PREFIX.$it" }

const val INTERCOM_DEFAULT_RABBIT_QUEUE_PREFIX = "ic"