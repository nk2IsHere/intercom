package eu.nk2.intercom.api

import eu.nk2.intercom.IntercomReturnBundle

interface IntercomReturnBundleSerializer {
    fun serialize(returnBundle: IntercomReturnBundle): ByteArray
    fun deserialize(data: ByteArray): IntercomReturnBundle?
}
