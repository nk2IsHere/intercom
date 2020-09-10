package eu.nk2.intercom.boot

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

const val INTERCOM_TCP_SERVER_BEAN_ID = "INTERCOM_TCP_SERVER_BEAN"
const val INTERCOM_TCP_CLIENT_BEAN_ID = "INTERCOM_TCP_CLIENT_BEAN"

val INTERCOM_ALLOWED_GENERIC_METHOD_RETURN_TYPES = arrayOf(Mono::class.java, Flux::class.java)