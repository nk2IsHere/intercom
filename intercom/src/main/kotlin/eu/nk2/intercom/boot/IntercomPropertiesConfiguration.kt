package eu.nk2.intercom.boot

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component

@NoArg
@ConstructorBinding
@Component
@ConfigurationProperties(prefix = "intercom")
data class IntercomPropertiesConfiguration(
    val serverMode: Boolean?,
    val serverSslSecurity: Boolean?,
    val serverAllowWiretapping: Boolean?,
    val host: String?,
    val port: Int?,
    val clientSslSecurity: Boolean?,
    val clientAllowWiretapping: Boolean?
)
