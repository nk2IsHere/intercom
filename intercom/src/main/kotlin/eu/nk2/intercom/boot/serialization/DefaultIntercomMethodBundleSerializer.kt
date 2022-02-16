package eu.nk2.intercom.boot.serialization

import com.fasterxml.jackson.databind.ObjectMapper
import eu.nk2.intercom.IntercomMethodBundle
import eu.nk2.intercom.api.IntercomMethodBundleSerializer

class DefaultIntercomMethodBundleSerializer(
    private val objectMapper: ObjectMapper
): IntercomMethodBundleSerializer {

    data class IntercomMethodSerializableBundle(
        val publisherId: Int,
        val methodId: Int,
        val parameters: IntercomBundleEntry
    )

    override fun deserialize(data: ByteArray): IntercomMethodBundle =
        objectMapper.readValue(data, 0, data.size, IntercomMethodSerializableBundle::class.java)
            .let {
                IntercomMethodBundle(
                    publisherId = it.publisherId,
                    methodId = it.methodId,
                    parameters = IntercomBundleEntry.expandEntry(it.parameters) as Array<*>
                )
            }

    override fun serialize(methodBundle: IntercomMethodBundle): ByteArray =
        objectMapper.writeValueAsBytes(
            IntercomMethodSerializableBundle(
            publisherId = methodBundle.publisherId,
            methodId = methodBundle.methodId,
            parameters = IntercomBundleEntry.collapseEntry(methodBundle.parameters)
        )
        )
}
