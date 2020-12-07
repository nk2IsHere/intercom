package eu.nk2.intercom

import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import org.springframework.stereotype.Component
import org.springframework.util.SerializationUtils

@Component
class DefaultIntercomMethodBundleSerializer: IntercomMethodBundleSerializer {

    override fun serialize(methodBundle: IntercomMethodBundle): ByteArray =
        SerializationUtils.serialize(methodBundle)!!

    override fun deserialize(data: ByteArray): IntercomMethodBundle? =
        SerializationUtils.deserialize(data) as? IntercomMethodBundle
}
