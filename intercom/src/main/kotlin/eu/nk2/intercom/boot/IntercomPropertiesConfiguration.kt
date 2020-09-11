package eu.nk2.intercom.boot

import eu.nk2.intercom.api.IntercomStarterMode
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component

@NoArg
@ConstructorBinding
@Component
@ConfigurationProperties(prefix = "intercom")
data class IntercomPropertiesConfiguration(
    val starterMode: IntercomStarterMode?,
    val host: String?,
    val port: Int?,
    val serverSslSecurity: Boolean?,
    val serverAllowWiretapping: Boolean?,
    val clientSslSecurity: Boolean?,
    val clientAllowWiretapping: Boolean?
)
