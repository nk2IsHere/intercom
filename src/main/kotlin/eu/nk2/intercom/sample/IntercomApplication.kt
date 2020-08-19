package eu.nk2.intercom.sample

import eu.nk2.intercom.ImportIntercom
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@ImportIntercom
@SpringBootApplication
class IntercomApplication

fun main(args: Array<String>) {
    runApplication<IntercomApplication>(*args)
}
