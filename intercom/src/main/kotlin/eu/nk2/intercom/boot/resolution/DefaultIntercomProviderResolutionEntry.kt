package eu.nk2.intercom.boot.resolution

import eu.nk2.intercom.api.IntercomProviderResolutionEntry

class DefaultIntercomProviderResolutionEntry(
    val host: String,
    val port: Int,
    id: String,
    type: Class<*>
): IntercomProviderResolutionEntry(
    id = id,
    type = type
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DefaultIntercomProviderResolutionEntry) return false
        if (!super.equals(other)) return false

        if (host != other.host) return false
        if (port != other.port) return false

        return true
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + host.hashCode()
        result = 31 * result + port
        return result
    }
}
