package eu.nk2.intercom.api

import java.net.URI

open class IntercomProviderResolutionRegistry<T: IntercomProviderResolutionEntry>(
    private val mutableEntryRegistry: MutableMap<String, T> = mutableMapOf(),
    private val mutableResolverRegistry: MutableSet<IntercomProviderResolver<T>> = mutableSetOf()
) {

    fun get(id: String): T? =
        mutableEntryRegistry[id]

    fun useResolver(resolver: IntercomProviderResolver<T>) {
        mutableResolverRegistry.add(resolver)
    }

    fun useEntry(id: String, uri: URI, type: Class<*>) {
        mutableEntryRegistry[id] = mutableResolverRegistry
            .firstOrNull { it.canResolveUri(uri) }
            ?.resolveUri(id, uri, type)
            ?: error("Cannot resolve uri $uri by any registered resolver")
    }
}

interface IntercomProviderResolver<T: IntercomProviderResolutionEntry> {
    fun canResolveUri(uri: URI): Boolean
    fun resolveUri(id: String, uri: URI, type: Class<*>): T
}

open class IntercomProviderResolutionEntry(
    val id: String,
    val type: Class<*>
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntercomProviderResolutionEntry) return false

        if (id != other.id) return false
        if (type != other.type) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + type.hashCode()
        return result
    }

    override fun toString(): String {
        return "IntercomProviderResolutionEntry(id='$id', type=$type)"
    }
}
