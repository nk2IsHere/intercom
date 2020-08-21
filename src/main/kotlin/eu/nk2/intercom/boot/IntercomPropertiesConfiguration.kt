package eu.nk2.intercom.boot

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "intercom")
class IntercomPropertiesConfiguration {
    var serverMode: Boolean? = null
    var host: String? = null
    var port: Int? = null
}
