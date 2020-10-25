package eu.nk2.intercom.utils

import java.util.*


fun Map<String, Any?>.toProperties(): Properties {
    val properties = Properties()
    properties.putAll(this)
    return properties
}