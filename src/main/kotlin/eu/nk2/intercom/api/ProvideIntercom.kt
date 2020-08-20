package eu.nk2.intercom.api

@Target(AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.RUNTIME)
annotation class ProvideIntercom(
    val id: String
)