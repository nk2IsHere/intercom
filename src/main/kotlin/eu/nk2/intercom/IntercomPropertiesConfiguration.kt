package eu.nk2.intercom

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "intercom")
class IntercomPropertiesConfiguration {
    var port: Int? = null
}
