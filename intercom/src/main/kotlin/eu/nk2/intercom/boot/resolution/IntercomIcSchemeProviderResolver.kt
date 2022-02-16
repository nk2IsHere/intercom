package eu.nk2.intercom.boot.resolution

import eu.nk2.intercom.api.IntercomProviderResolver
import eu.nk2.intercom.boot.INTERCOM_DEFAULT_SERVER_PORT
import java.net.URI

class IntercomIcSchemeProviderResolver: IntercomProviderResolver<DefaultIntercomProviderResolutionEntry> {

    override fun canResolveUri(uri: URI): Boolean =
        uri.scheme == "ic"

    override fun resolveUri(id: String, uri: URI, type: Class<*>): DefaultIntercomProviderResolutionEntry =
        DefaultIntercomProviderResolutionEntry(
            host = uri.host,
            port = if(uri.port < 0) INTERCOM_DEFAULT_SERVER_PORT else uri.port,
            id = id,
            type = type
        )
}
