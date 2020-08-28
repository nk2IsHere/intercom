package eu.nk2.intercom.api

import eu.nk2.intercom.IntercomMethodBundle

interface IntercomMethodBundleSerializer {
    fun serialize(methodBundle: IntercomMethodBundle): ByteArray
    fun deserialize(data: ByteArray): IntercomMethodBundle?
}
