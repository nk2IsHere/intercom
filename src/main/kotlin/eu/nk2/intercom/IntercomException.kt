package eu.nk2.intercom

import java.io.Serializable

class IntercomException(val msg: String, val error: IntercomError): Exception("$error: $msg")

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