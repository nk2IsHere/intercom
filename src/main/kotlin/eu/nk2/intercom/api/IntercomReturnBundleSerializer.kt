package eu.nk2.intercom.api

import eu.nk2.intercom.IntercomReturnBundle

interface IntercomReturnBundleSerializer {
    fun <T> serialize(returnBundle: IntercomReturnBundle<T>): ByteArray
    fun <T> deserialize(data: ByteArray): IntercomReturnBundle<T>?
}
