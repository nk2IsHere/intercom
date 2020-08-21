package eu.nk2.intercom

import java.io.Serializable

data class IntercomReturnBundle<T>(
    val error: IntercomError?,
    val data: T?
): Serializable {
    enum class IntercomError: Serializable {
        NO_DATA,
        BAD_DATA,
        BAD_PUBLISHER,
        BAD_METHOD,
        BAD_PARAMS,
        PROVIDER_ERROR,
        INTERNAL_ERROR;

        companion object {
            private const val serialVersionUID = 20200721001507L
        }
    }

    companion object {
        private const val serialVersionUID = 20200720231934L
    }
}
