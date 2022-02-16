package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomStarterMode
import eu.nk2.intercom.utils.NoArgsConstructor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.net.URI

@NoArgsConstructor
@ConstructorBinding
@ConfigurationProperties(prefix = INTERCOM_PROPERTIES_PREFIX)
data class IntercomPropertiesConfiguration(
    val starterMode: IntercomStarterMode?
)

@NoArgsConstructor
@ConstructorBinding
@ConfigurationProperties(prefix = INTERCOM_SERVER_PROPERTIES_PREFIX)
data class IntercomServerPropertiesConfiguration(
    val port: Int?,
    val sslSecurity: Boolean?,
    val allowWiretapping: Boolean?
)

@NoArgsConstructor
@ConstructorBinding
@ConfigurationProperties(prefix = INTERCOM_CLIENT_PROPERTIES_PREFIX)
data class IntercomClientPropertiesConfiguration(
    val sslSecurity: Boolean?,
    val allowWiretapping: Boolean?,
    val connectionTimeoutMillis: Int?,
    val routes: List<IntercomClientRoutesEntryPropertiesConfiguration>?
)

data class IntercomClientRoutesEntryPropertiesConfiguration(
    val id: String,
    val uri: URI,
    val type: Class<*>
)
