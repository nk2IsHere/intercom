package eu.nk2.intercom.serialization

import eu.nk2.intercom.*
import eu.nk2.kjackson.jsonDeserializer
import eu.nk2.kjackson.jsonObject
import eu.nk2.kjackson.jsonSerializer
import eu.nk2.kjackson.string

val IntercomErrorSerializer = jsonSerializer<IntercomError> { src, context -> jsonObject(
    "type" to src::class.qualifiedName,
    "message" to src.message,
    "should" to if(src is BadParamsIntercomError) src.should else null,
    "had" to if(src is BadParamsIntercomError) src.had else null,
    "throwable" to if(src is IntercomThrowableAwareError) context(IntercomBundleEntry.collapseEntry(src.throwable)) else null
) }

val IntercomErrorDeserializer = jsonDeserializer { src, context -> when(val type = src["type"].string) {
    ProviderIntercomError::class.qualifiedName -> ProviderIntercomError(IntercomBundleEntry.expandEntry(context(src["throwable"])) as Throwable)
    InternalIntercomError::class.qualifiedName -> InternalIntercomError(IntercomBundleEntry.expandEntry(context(src["throwable"])) as Throwable)
    BadParamsIntercomError::class.qualifiedName -> BadParamsIntercomError()
    else -> IntercomError::class.sealedSubclasses.find { it.qualifiedName == type }
        ?.objectInstance ?: error("IntercomError is not an object: $type")
} }