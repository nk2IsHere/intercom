package eu.nk2.intercom.boot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomStarterMode
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.serialization.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration
import org.springframework.boot.autoconfigure.amqp.RabbitProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import reactor.core.publisher.Mono
import java.util.stream.Stream

import org.springframework.core.type.AnnotatedTypeMetadata

import org.springframework.context.annotation.ConditionContext




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

    @Bean(INTERCOM_JACKSON_BEAN_ID)
    @Order(Ordered.LOWEST_PRECEDENCE)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Conditional(IntercomObjectMapperBeanExistsCondition::class)
    fun intercomObjectMapper(
    ): ObjectMapper =
        jacksonObjectMapper()
            .registerModule(
                SimpleModule()
                    .addSerializer(IntercomBundleEntry::class.java, IntercomBundleEntry.serializer)
                    .addDeserializer(IntercomBundleEntry::class.java, IntercomBundleEntry.deserializer)
                    .addSerializer(IntercomError::class.java, IntercomErrorSerializer)
                    .addDeserializer(IntercomError::class.java, IntercomErrorDeserializer)
            )

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean(IntercomMethodBundleSerializer::class)
    fun intercomMethodBundleSerializer(
        @Autowired @Qualifier(INTERCOM_JACKSON_BEAN_ID) objectMapper: ObjectMapper
    ): IntercomMethodBundleSerializer =
        DefaultIntercomMethodBundleSerializer(
            objectMapper = objectMapper
        )

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean(IntercomReturnBundleSerializer::class)
    fun intercomReturnBundleSerializer(
        @Autowired @Qualifier(INTERCOM_JACKSON_BEAN_ID) objectMapper: ObjectMapper
    ): IntercomReturnBundleSerializer =
        DefaultIntercomReturnBundleSerializer(
            objectMapper = objectMapper
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

internal class IntercomObjectMapperBeanExistsCondition: Condition {
    override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean =
        context.beanFactory!!
            .getBeanNamesForType(ObjectMapper::class.java)
            .any { it == INTERCOM_JACKSON_BEAN_ID }
            .not()
}