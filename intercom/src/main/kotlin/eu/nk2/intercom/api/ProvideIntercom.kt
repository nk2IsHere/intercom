package eu.nk2.intercom.api

@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProvideIntercom(
    val id: String
)
