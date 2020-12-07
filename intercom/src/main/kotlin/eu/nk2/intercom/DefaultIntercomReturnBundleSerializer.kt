package eu.nk2.intercom

import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import org.springframework.stereotype.Component
import org.springframework.util.SerializationUtils

@Component
class DefaultIntercomReturnBundleSerializer: IntercomReturnBundleSerializer {

    override fun <T> serialize(returnBundle: IntercomReturnBundle<T>): ByteArray =
        SerializationUtils.serialize(returnBundle)!!

    override fun <T> deserialize(data: ByteArray): IntercomReturnBundle<T>? =
        SerializationUtils.deserialize(data) as? IntercomReturnBundle<T>
}
