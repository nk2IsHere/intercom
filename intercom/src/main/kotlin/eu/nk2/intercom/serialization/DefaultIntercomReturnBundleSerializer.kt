package eu.nk2.intercom.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.IntercomReturnBundleSerializer

class DefaultIntercomReturnBundleSerializer(
    private val objectMapper: ObjectMapper
): IntercomReturnBundleSerializer {

    data class IntercomReturnSerializableBundle(
        val error: IntercomError?,
        val data: IntercomBundleEntry
    )

    override fun deserialize(data: ByteArray): IntercomReturnBundle =
        objectMapper.readValue(data, 0, data.size, IntercomReturnSerializableBundle::class.java)
            .let {
                IntercomReturnBundle(
                    error = it.error,
                    data = IntercomBundleEntry.expandEntry(it.data)
                )
            }

    override fun serialize(returnBundle: IntercomReturnBundle): ByteArray =
        objectMapper.writeValueAsBytes(IntercomReturnSerializableBundle(
            error = returnBundle.error,
            data = IntercomBundleEntry.collapseEntry(returnBundle.data)
        ))
}