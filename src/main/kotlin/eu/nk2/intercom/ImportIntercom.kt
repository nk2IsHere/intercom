package eu.nk2.intercom

import org.springframework.context.annotation.Import

@Import(IntercomConfiguration::class)
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class ImportIntercom