package eu.nk2.intercom.boot

import org.springframework.context.annotation.Import

@Import(IntercomAutoConfiguration::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class EnableIntercom