package eu.nk2.intercom

import eu.nk2.intercom.utils.NoArgsConstructor
import java.io.Serializable

@NoArgsConstructor
data class IntercomReturnBundle(
    val error: IntercomError?,
    val data: Any?
): Serializable {

    companion object {
        private const val serialVersionUID = 20200720231934L
    }
}
