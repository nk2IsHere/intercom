package eu.nk2.intercom.boot

import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

const val INTERCOM_TCP_CLIENT_BEAN_ID = "intercomTcpClient"
const val INTERCOM_TCP_SERVER_BEAN_ID = "intercomTcpServer"
const val INTERCOM_JACKSON_BEAN_ID = "intercomJackson"

internal val INTERCOM_ALLOWED_GENERIC_METHOD_RETURN_TYPES = arrayOf(Mono::class.java, Flux::class.java, Publisher::class.java)

internal const val INTERCOM_PROPERTIES_PREFIX = "intercom"
internal const val INTERCOM_CLIENT_PROPERTIES_PREFIX = "intercom.client"
internal const val INTERCOM_SERVER_PROPERTIES_PREFIX = "intercom.server"

internal val INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES = arrayOf("starter_mode", "starter-mode", "starterMode")
    .map { "$INTERCOM_PROPERTIES_PREFIX.$it" }

internal const val INTERCOM_DEFAULT_SERVER_PORT = 5000

internal const val INTERCOM_DEFAULT_CONNECTION_TIMEOUT = 5000

internal const val INTERCOM_CLIENT_ROUTES_PROPERTIES_PREFIX = "intercom.client.routes"
