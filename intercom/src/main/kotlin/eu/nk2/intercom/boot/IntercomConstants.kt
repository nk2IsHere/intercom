package eu.nk2.intercom.boot

import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

const val INTERCOM_KAFKA_STREAM_PROPERTIES_BEAN_ID = "intercomKafkaStreamProperties"
const val INTERCOM_KAFKA_TOPIC_PREFIX_KEY = "topicPrefix"
const val INTERCOM_KAFKA_TOPIC_PARTITION_NUMBER_KEY = "partitionNumber"
const val INTERCOM_KAFKA_TOPIC_REPLICATION_FACTOR_KEY = "replicationFactor"

internal val INTERCOM_ALLOWED_GENERIC_METHOD_RETURN_TYPES = arrayOf(Mono::class.java, Flux::class.java)

internal const val INTERCOM_PROPERTIES_PREFIX = "intercom"

val INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES = arrayOf("starter_mode", "starter-mode", "starterMode")
    .map { "$INTERCOM_PROPERTIES_PREFIX.$it" }