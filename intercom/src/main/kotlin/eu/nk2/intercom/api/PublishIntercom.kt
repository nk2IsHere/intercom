package eu.nk2.intercom.api

import org.springframework.stereotype.Service

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Service
annotation class PublishIntercom(
    val id: String
)
