package eu.nk2.intercom.api

import org.springframework.stereotype.Service
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Service
annotation class PublishIntercom(
    val id: String,
    val type: KClass<*> = Unit::class
)
