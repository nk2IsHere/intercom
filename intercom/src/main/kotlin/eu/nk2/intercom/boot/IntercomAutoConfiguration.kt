package eu.nk2.intercom.boot

import eu.nk2.intercom.DefaultIntercomMethodBundleSerializer
import eu.nk2.intercom.DefaultIntercomReturnBundleSerializer
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.IntercomStarterMode
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*


@Configuration
@EnableConfigurationProperties(IntercomPropertiesConfiguration::class)
@Conditional(IntercomAutoConfigurationEnabledCondition::class)
class IntercomAutoConfiguration {

    @Bean(INTERCOM_KAFKA_STREAM_PROPERTIES_BEAN_ID)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun intercomKafkaStreamProperties(
        @Autowired properties: IntercomPropertiesConfiguration
    ): Map<String, Any> = mapOf(
//        ProducerConfig.APPLICATION_ID_CONFIG to (properties.kafkaApplicationId ?: error("intercom.kafkaApplicationId is required")),
        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to (properties.kafkaBrokers ?: error("intercom.kafkaBrokers is required")),
        ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
        ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to ByteArraySerializer::class.java,
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to ByteArrayDeserializer::class.java,
//        ProducerConfig.ACKS_CONFIG to "0",
//        ProducerConfig.RETRIES_CONFIG to "0",
        ProducerConfig.ACKS_CONFIG to "all",

        INTERCOM_KAFKA_TOPIC_PREFIX_KEY to (properties.kafkaTopicPrefix?: error("intercom.kafkaTopicPrefix is required")),
        INTERCOM_KAFKA_TOPIC_PARTITION_NUMBER_KEY to (properties.kafkaPartitionNumber ?: error("intercom.kafkaPartitionNumber is required")),
        INTERCOM_KAFKA_TOPIC_REPLICATION_FACTOR_KEY to (properties.kafkaReplicationFactor ?: error("intercom.kafkaReplicationFactor is required"))
    )


    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Conditional(IntercomAutoConfigurationServerEnabledCondition::class)
    fun intercomPublisherBeanPostProcessor(
        @Autowired @Qualifier(INTERCOM_KAFKA_STREAM_PROPERTIES_BEAN_ID) kafkaStreamProperties: Map<String, Any>,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomPublisherBeanPostProcessor =
        IntercomPublisherBeanPostProcessor(
            kafkaStreamProperties = kafkaStreamProperties,
            intercomMethodBundleSerializer = intercomMethodBundleSerializer,
            intercomReturnBundleSerializer = intercomReturnBundleSerializer
        )

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @Conditional(IntercomAutoConfigurationClientEnabledCondition::class)
    fun intercomProviderBeanPostProcessor(
        @Autowired properties: IntercomPropertiesConfiguration,
        @Autowired @Qualifier(INTERCOM_KAFKA_STREAM_PROPERTIES_BEAN_ID) kafkaStreamProperties: Map<String, Any>,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomProviderBeanPostProcessor =
        IntercomProviderBeanPostProcessor(
            kafkaStreamProperties = kafkaStreamProperties,
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
    { IntercomStarterMode.valueOf(it) },
    listOf(
        null,
        IntercomStarterMode.CLIENT_ONLY,
        IntercomStarterMode.CLIENT_SERVER,
        IntercomStarterMode.SERVER_ONLY
    )
)

internal class IntercomAutoConfigurationServerEnabledCondition: IntercomPropertyCondition<IntercomStarterMode?>(
    INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES,
    { IntercomStarterMode.valueOf(it) },
    listOf(IntercomStarterMode.SERVER_ONLY, IntercomStarterMode.CLIENT_SERVER)
)

internal class IntercomAutoConfigurationClientEnabledCondition: IntercomPropertyCondition<IntercomStarterMode?>(
    INTERCOM_STARTER_MODE_ACCEPTED_PROPERTY_NAMES,
    { IntercomStarterMode.valueOf(it) },
    listOf(null, IntercomStarterMode.CLIENT_ONLY, IntercomStarterMode.CLIENT_SERVER)
)
