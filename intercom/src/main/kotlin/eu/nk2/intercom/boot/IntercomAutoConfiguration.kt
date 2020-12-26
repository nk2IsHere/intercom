package eu.nk2.intercom.boot

import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import eu.nk2.intercom.DefaultIntercomMethodBundleSerializer
import eu.nk2.intercom.DefaultIntercomReturnBundleSerializer
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.IntercomStarterMode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import reactor.core.publisher.Mono

@Configuration
@Import(RabbitAutoConfiguration::class)
@EnableConfigurationProperties(IntercomPropertiesConfiguration::class)
@Conditional(IntercomAutoConfigurationEnabledCondition::class)
class IntercomAutoConfiguration {

    @Bean(INTERCOM_RABBIT_CONNECTION_BEAN_ID)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun intercomRabbitConnection(
        @Autowired rabbitProperties: RabbitProperties
    ): Mono<Connection> = Mono.fromCallable {
        ConnectionFactory().apply {
            host = rabbitProperties.determineHost()
            port = rabbitProperties.determinePort()
            username = rabbitProperties.determineUsername()
            password = rabbitProperties.determinePassword()
        }.newConnection()
    }.cache()

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun intercomProcessorProperties(
        @Autowired properties: IntercomPropertiesConfiguration,
        @Autowired @Qualifier(INTERCOM_RABBIT_CONNECTION_BEAN_ID) rabbitConnection: Mono<Connection>
    ) = IntercomProcessorProperties(
        connection = rabbitConnection,
        queuePrefix = properties.rabbitQueuePrefix ?: INTERCOM_DEFAULT_RABBIT_QUEUE_PREFIX,
        timeoutMillis = properties.timeoutMillis ?: INTERCOM_DEFAULT_TIMEOUT
    )

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Conditional(IntercomAutoConfigurationServerEnabledCondition::class)
    fun intercomPublisherBeanPostProcessor(
        @Autowired intercomProcessorProperties: IntercomProcessorProperties,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomPublisherBeanPostProcessor =
        IntercomPublisherBeanPostProcessor(
            intercomProcessorProperties = intercomProcessorProperties,
            intercomMethodBundleSerializer = intercomMethodBundleSerializer,
            intercomReturnBundleSerializer = intercomReturnBundleSerializer
        )

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Conditional(IntercomAutoConfigurationClientEnabledCondition::class)
    fun intercomProviderBeanPostProcessor(
        @Autowired intercomProcessorProperties: IntercomProcessorProperties,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomProviderBeanPostProcessor =
        IntercomProviderBeanPostProcessor(
            intercomProcessorProperties = intercomProcessorProperties,
            intercomMethodBundleSerializer = intercomMethodBundleSerializer,
            intercomReturnBundleSerializer = intercomReturnBundleSerializer
        )

    @Bean
    @ConditionalOnMissingBean(IntercomMethodBundleSerializer::class)
    fun intercomMethodBundleSerializer(
    ): IntercomMethodBundleSerializer =
        DefaultIntercomMethodBundleSerializer(
        )

    @Bean
    @ConditionalOnMissingBean(IntercomReturnBundleSerializer::class)
    fun intercomReturnBundleSerializer(
    ): IntercomReturnBundleSerializer =
        DefaultIntercomReturnBundleSerializer(
        )
}

internal class IntercomAutoConfigurationEnabledCondition: IntercomPropertyCondition<IntercomStarterMode?>(
    INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES,
    { IntercomStarterMode.valueOf(it.toUpperCase()) },
    listOf(
        IntercomStarterMode.CLIENT_ONLY,
        IntercomStarterMode.CLIENT_SERVER,
        IntercomStarterMode.SERVER_ONLY
    )
)

internal class IntercomAutoConfigurationServerEnabledCondition: IntercomPropertyCondition<IntercomStarterMode?>(
    INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES,
    { IntercomStarterMode.valueOf(it.toUpperCase()) },
    listOf(IntercomStarterMode.SERVER_ONLY, IntercomStarterMode.CLIENT_SERVER)
)

internal class IntercomAutoConfigurationClientEnabledCondition: IntercomPropertyCondition<IntercomStarterMode?>(
    INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES,
    { IntercomStarterMode.valueOf(it.toUpperCase()) },
    listOf(IntercomStarterMode.CLIENT_ONLY, IntercomStarterMode.CLIENT_SERVER)
)
