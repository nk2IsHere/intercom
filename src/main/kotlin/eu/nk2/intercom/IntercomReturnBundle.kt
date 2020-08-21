package eu.nk2.intercom

import java.io.Serializable

data class IntercomReturnBundle<T>(
    val error: IntercomError?,
    val data: T?
): Serializable {
    companion object {
        private const val serialVersionUID = 20200720231934L
    }
}
