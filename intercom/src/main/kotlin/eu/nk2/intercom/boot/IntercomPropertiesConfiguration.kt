package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomStarterMode
import eu.nk2.intercom.utils.NoArg
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.stereotype.Component

@NoArg
@ConstructorBinding
@Component
@ConfigurationProperties(prefix = INTERCOM_PROPERTIES_PREFIX)
data class IntercomPropertiesConfiguration(
    val starterMode: IntercomStarterMode?,
    val rabbitQueuePrefix: String?
)
