package eu.nk2.intercom

import java.io.Serializable

class IntercomException(val error: IntercomError): Exception("$error: ${error.message}")

enum class IntercomError(val message: String): Serializable {
    NO_DATA("Server received no data"),
    BAD_DATA("Server received bad data"),
    BAD_PUBLISHER("Server received bad publisher - it cannot be found"),
    BAD_METHOD("Server received bad method - it cannot be found"),
    BAD_PARAMS("Server received bad parameters - args count or types mismatch"),
    PROVIDER_ERROR("Server produced provider error - check logs"),
    INTERNAL_ERROR("Server received internal error - check logs and mentally punch the author");

    companion object {
        private const val serialVersionUID = 20200721001507L
    }
}