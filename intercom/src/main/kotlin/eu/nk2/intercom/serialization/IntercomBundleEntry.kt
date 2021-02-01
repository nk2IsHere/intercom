package eu.nk2.intercom.serialization

import eu.nk2.kjackson.*
import java.util.concurrent.ConcurrentHashMap

data class IntercomBundleEntry(
    val entry: Any?,
    val clazz: Class<out Any>?,
    val type: IntercomBundleEntryType
) {
    enum class IntercomBundleEntryType {
        NULL,
        COMMON,
        ARRAY,
        LIST,
        MAP
    }

    @Suppress("UNCHECKED_CAST")
    companion object {
        private val classCache = ConcurrentHashMap<String, Class<*>>()

        private fun <T> getClassByCache(className: String): Class<T> =
            (classCache[className] as? Class<T>) ?: (Class.forName(className) as? Class<T>)
                ?.apply { classCache[className] = this }
            ?: error("can't cast $className to its type")

        val serializer = jsonSerializer<IntercomBundleEntry> { src, context -> jsonObject(
            "entry" to context(src.entry),
            "class" to src.clazz?.name?.toJson(),
            "type" to src.type.name
        ) }

        val deserializer = jsonDeserializer { src, context ->
            val type = IntercomBundleEntryType.valueOf(src["type"].string)
            val clazz = if(!src["class"].isNull) getClassByCache<Any>(src["class"].string) else null
            IntercomBundleEntry(
                entry = when (type) {
                    IntercomBundleEntryType.NULL -> null
                    IntercomBundleEntryType.COMMON -> context(src["entry"], clazz!!)
                    IntercomBundleEntryType.ARRAY -> src["entry"].array.map { context<IntercomBundleEntry>(it) }
                    IntercomBundleEntryType.LIST -> src["entry"].array.map { context<IntercomBundleEntry>(it) }
                    IntercomBundleEntryType.MAP -> src["entry"].obj.toMap().mapValues { (_, value) -> context<IntercomBundleEntry>(value) }
                },
                type = type,
                clazz = clazz
            )
        }

        fun collapseEntry(data: Any?): IntercomBundleEntry =
            when(data) {
                is Array<*> -> IntercomBundleEntry(
                    entry = data.map { collapseEntry(it) },
                    clazz = List::class.java,
                    type = IntercomBundleEntryType.ARRAY
                )
                is Iterable<*> -> IntercomBundleEntry(
                    entry = data.map { collapseEntry(it) },
                    clazz = List::class.java,
                    type = IntercomBundleEntryType.LIST
                )
                is Map<*, *> -> IntercomBundleEntry(
                    entry = data.map { (key, value) -> (key as String) to collapseEntry(value) }
                        .toMap(),
                    clazz = Map::class.java,
                    type = IntercomBundleEntryType.MAP
                )
                null -> IntercomBundleEntry(
                    entry = null,
                    clazz = null,
                    type = IntercomBundleEntryType.NULL
                )
                else -> IntercomBundleEntry(
                    entry = data,
                    clazz = data.javaClass,
                    type = IntercomBundleEntryType.COMMON
                )
            }

        fun expandEntry(entry: IntercomBundleEntry): Any? =
            when(entry.type) {
                IntercomBundleEntryType.NULL -> null
                IntercomBundleEntryType.COMMON -> entry.entry
                IntercomBundleEntryType.ARRAY -> (entry.entry as Iterable<IntercomBundleEntry>).map { expandEntry(it) }.toTypedArray()
                IntercomBundleEntryType.LIST -> (entry.entry as Iterable<IntercomBundleEntry>).map { expandEntry(it) }
                IntercomBundleEntryType.MAP -> (entry.entry as Map<String, IntercomBundleEntry>).mapValues { (_, value) -> expandEntry(value) }
            }
    }

}
