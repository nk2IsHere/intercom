package eu.nk2.intercom.boot

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomStarterMode
import eu.nk2.intercom.api.*
import eu.nk2.intercom.boot.resolution.DefaultIntercomProviderResolutionEntry
import eu.nk2.intercom.boot.resolution.DefaultIntercomProviderResolutionRegistry
import eu.nk2.intercom.boot.resolution.IntercomIcSchemeProviderResolver
import eu.nk2.intercom.boot.serialization.*
import eu.nk2.intercom.boot.stream.DefaultIntercomProviderStreamFactory
import eu.nk2.intercom.boot.stream.DefaultIntercomPublisherStreamFactory
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.*
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.core.type.AnnotatedTypeMetadata
import org.springframework.context.annotation.ConditionContext
import reactor.netty.tcp.TcpClient
import reactor.netty.tcp.TcpServer


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


@Configuration
@EnableConfigurationProperties(
    IntercomPropertiesConfiguration::class,
    IntercomClientPropertiesConfiguration::class,
    IntercomServerPropertiesConfiguration::class
)
@Conditional(IntercomAutoConfigurationEnabledCondition::class)
class IntercomAutoConfiguration {

    @Bean
    @Conditional(IntercomAutoConfigurationClientEnabledCondition::class)
    fun intercomIcSchemeProviderResolver(): IntercomIcSchemeProviderResolver =
        IntercomIcSchemeProviderResolver()

    @Bean
    @ConditionalOnMissingBean(IntercomProviderResolutionRegistry::class)
    @Conditional(IntercomAutoConfigurationClientEnabledCondition::class)
    fun intercomProviderResolutionRegistry(
        properties: IntercomClientPropertiesConfiguration,
        providers: List<IntercomProviderResolver<DefaultIntercomProviderResolutionEntry>>
    ): IntercomProviderResolutionRegistry<DefaultIntercomProviderResolutionEntry> =
        DefaultIntercomProviderResolutionRegistry()
            .apply {
                providers.forEach { useResolver(it) }
                properties.routes.forEach { useEntry(it.id, it.uri, it.type) }
            }

    @Bean(INTERCOM_TCP_SERVER_BEAN_ID)
    @Conditional(IntercomAutoConfigurationServerEnabledCondition::class)
    fun intercomTcpServer(
        properties: IntercomServerPropertiesConfiguration
    ): TcpServer =
        TcpServer.create()
            .option(ChannelOption.AUTO_CLOSE, true)
            .port(properties.port ?: INTERCOM_DEFAULT_SERVER_PORT)
            .wiretap(properties.allowWiretapping ?: false)
            .let {
                if(properties.sslSecurity == true)
                    it.secure {
                        with(SelfSignedCertificate()) {
                            it.sslContext(
                                SslContextBuilder
                                    .forServer(
                                        this.certificate(),
                                        this.privateKey()
                                    )
                            )
                        }
                    }
                else
                    it
            }

    @Bean(INTERCOM_TCP_CLIENT_BEAN_ID)
    @Conditional(IntercomAutoConfigurationClientEnabledCondition::class)
    fun intercomTcpClient(
        properties: IntercomClientPropertiesConfiguration
    ): TcpClient =
        TcpClient.create()
            .option(ChannelOption.AUTO_CLOSE, true)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, properties.connectionTimeoutMillis ?: INTERCOM_DEFAULT_CONNECTION_TIMEOUT)
            .wiretap(properties.allowWiretapping ?: false)
            .let {
                if(properties.sslSecurity == true)
                    it.secure {
                        it.sslContext(
                            SslContextBuilder
                                .forClient()
                                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        )
                    }
                else
                    it
            }

    @Bean
    @ConditionalOnMissingBean(IntercomPublisherStreamFactory::class)
    @Conditional(IntercomAutoConfigurationServerEnabledCondition::class)
    fun intercomPublisherStreamFactory(
        @Qualifier(INTERCOM_TCP_SERVER_BEAN_ID) tcpServer: TcpServer,
        intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomPublisherStreamFactory = DefaultIntercomPublisherStreamFactory(
        tcpServer = tcpServer,
        methodBundleSerializer = intercomMethodBundleSerializer,
        returnBundleSerializer = intercomReturnBundleSerializer
    )

    @Bean
    @ConditionalOnMissingBean(IntercomProviderStreamFactory::class)
    @Conditional(IntercomAutoConfigurationClientEnabledCondition::class)
    fun intercomProviderStreamFactory(
        @Qualifier(INTERCOM_TCP_CLIENT_BEAN_ID) tcpClient: TcpClient,
        intercomProviderResolutionRegistry: IntercomProviderResolutionRegistry<DefaultIntercomProviderResolutionEntry>,
        intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomProviderStreamFactory = DefaultIntercomProviderStreamFactory(
        tcpClient = tcpClient,
        providerResolutionRegistry = intercomProviderResolutionRegistry,
        methodBundleSerializer = intercomMethodBundleSerializer,
        returnBundleSerializer = intercomReturnBundleSerializer
    )

    @Bean
    @Conditional(IntercomAutoConfigurationServerEnabledCondition::class)
    fun intercomPublisherBeanPostProcessor(
        intercomPublisherStreamFactory: IntercomPublisherStreamFactory
    ): IntercomPublisherBeanPostProcessor =
        IntercomPublisherBeanPostProcessor(
            publisherStreamFactory = intercomPublisherStreamFactory
        )

    @Bean
    @Conditional(IntercomAutoConfigurationClientEnabledCondition::class)
    fun intercomProviderBeanPostProcessor(
        intercomProviderStreamFactory: IntercomProviderStreamFactory
    ): IntercomProviderBeanPostProcessor =
        IntercomProviderBeanPostProcessor(
            providerStreamFactory = intercomProviderStreamFactory
        )

    @Bean(INTERCOM_JACKSON_BEAN_ID)
    @Order(Ordered.LOWEST_PRECEDENCE)
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
        @Qualifier(INTERCOM_JACKSON_BEAN_ID) objectMapper: ObjectMapper
    ): IntercomMethodBundleSerializer =
        DefaultIntercomMethodBundleSerializer(
            objectMapper = objectMapper
        )

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    @ConditionalOnMissingBean(IntercomReturnBundleSerializer::class)
    fun intercomReturnBundleSerializer(
        @Qualifier(INTERCOM_JACKSON_BEAN_ID) objectMapper: ObjectMapper
    ): IntercomReturnBundleSerializer =
        DefaultIntercomReturnBundleSerializer(
            objectMapper = objectMapper
        )
}
