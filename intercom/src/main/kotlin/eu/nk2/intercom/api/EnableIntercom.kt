package eu.nk2.intercom.api

import eu.nk2.intercom.boot.IntercomAutoConfiguration
import org.springframework.context.annotation.Import

@Import(IntercomAutoConfiguration::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableIntercom