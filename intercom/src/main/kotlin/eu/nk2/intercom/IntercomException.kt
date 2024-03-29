package eu.nk2.intercom

import java.io.Serializable

class IntercomException(val error: IntercomError): Exception("$error: ${error.message}")

sealed class IntercomError(val message: String): Serializable {
    companion object {
        private const val serialVersionUID = 20200721001507L
    }
}

sealed class IntercomThrowableAwareError(message: String, val throwable: Throwable): IntercomError(message)

object NoDataIntercomError: IntercomError("Server received no data")
object BadDataIntercomError: IntercomError("Server received bad data")
object BadPublisherIntercomError :IntercomError("Server received bad publisher - it cannot be found")
object BadMethodIntercomError: IntercomError("Server received bad method - it cannot be found")
object BadMethodReturnTypeIntercomError: IntercomError("Server has a bad method - it is not Publisher")
class BadParamsIntercomError(val should: String?, val had: String?): IntercomError(
    if(should != null && had != null) "Server received bad parameters - types mismatch (should: $should, had: $had)"
    else "Server received bad parameters - args count mismatch"
) {
    constructor(should: Class<*>? = null, had: Class<*>? = null): this(should?.name, had?.name)
}
class ProviderIntercomError(throwable: Throwable): IntercomThrowableAwareError("Server produced provider error - check logs", throwable)
class InternalIntercomError(throwable: Throwable): IntercomThrowableAwareError("Server received internal error - check logs and mentally punch the author of Intercom", throwable)
object ClientNoDataIntercomError: IntercomError("Client received no data")
object UnreachableIntercomError: IntercomError("This error must be unreachable, if you see it blame nk2")
class NoProviderResolutionEntry(val id: String): IntercomError("No provider resolved by id $id - check your routes configuration")
