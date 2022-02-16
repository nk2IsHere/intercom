package eu.nk2.intercom.api

@Deprecated("Use beans defined by routes ids instead (when they are done)")
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProvideIntercom(
    val id: String
)
