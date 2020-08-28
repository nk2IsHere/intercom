package eu.nk2.intercom.perf

import eu.nk2.intercom.boot.EnableIntercom
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@EnableIntercom
@EnableWebFlux
@SpringBootApplication
class PerfApplication

fun main(args: Array<String>) {
	runApplication<PerfApplication>(*args)
}
