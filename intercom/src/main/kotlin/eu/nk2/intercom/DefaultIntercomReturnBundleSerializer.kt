package eu.nk2.intercom

import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import org.springframework.stereotype.Component
import org.springframework.util.SerializationUtils

@Component
class DefaultIntercomReturnBundleSerializer: IntercomReturnBundleSerializer {

    override fun serialize(returnBundle: IntercomReturnBundle): ByteArray =
        SerializationUtils.serialize(returnBundle)!!

    override fun deserialize(data: ByteArray): IntercomReturnBundle? =
        SerializationUtils.deserialize(data) as? IntercomReturnBundle
}
