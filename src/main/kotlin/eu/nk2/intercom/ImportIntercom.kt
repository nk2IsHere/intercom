package eu.nk2.intercom

import org.springframework.context.annotation.Import

@Import(IntercomAutoConfiguration::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImportIntercom