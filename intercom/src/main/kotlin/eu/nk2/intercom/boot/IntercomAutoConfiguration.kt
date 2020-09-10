package eu.nk2.intercom.boot

import eu.nk2.intercom.DefaultIntercomMethodBundleSerializer
import eu.nk2.intercom.DefaultIntercomReturnBundleSerializer
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import io.netty.channel.ChannelOption
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope
import reactor.netty.tcp.TcpClient
import reactor.netty.tcp.TcpServer

@Configuration
@EnableConfigurationProperties(IntercomPropertiesConfiguration::class)
@ConditionalOnProperty(prefix = "intercom", name = ["server-mode", "host", "port"])
class IntercomAutoConfiguration {

    @Bean(INTERCOM_TCP_SERVER_BEAN_ID)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(prefix = "intercom", name = ["server-mode"], havingValue = "true", matchIfMissing = true)
    fun intercomTcpServer(
        @Autowired properties: IntercomPropertiesConfiguration
    ): TcpServer =
        TcpServer.create()
            .option(ChannelOption.AUTO_CLOSE, true)
            .port(properties.port ?: error("Intercom requires server port to be present in configuration"))
            .wiretap(properties.serverAllowWiretapping ?: false)
            .let {
                if(properties.serverSslSecurity == true) it.secure { SelfSignedCertificate().apply {
                    it.sslContext(SslContextBuilder.forServer(this.certificate(), this.privateKey()))
                } }
                else it
            }


    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    @ConditionalOnProperty(prefix = "intercom", name = ["server-mode"], havingValue = "true", matchIfMissing = true)
    fun intercomPublisherBeanPostProcessor(
        @Autowired @Qualifier(INTERCOM_TCP_SERVER_BEAN_ID) tcpServer: TcpServer,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomPublisherBeanPostProcessor =
        IntercomPublisherBeanPostProcessor(
            tcpServer = tcpServer,
            intercomMethodBundleSerializer = intercomMethodBundleSerializer,
            intercomReturnBundleSerializer = intercomReturnBundleSerializer
        )

    @Bean(INTERCOM_TCP_CLIENT_BEAN_ID)
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun tcpClient(
        @Autowired properties: IntercomPropertiesConfiguration
    ): TcpClient =
        TcpClient.create()
            .option(ChannelOption.AUTO_CLOSE, true)
            .host(properties.host ?: error("Intercom requires client host to be present in configuration"))
            .port(properties.port ?: error("Intercom requires client port to be present in configuration"))
            .wiretap(properties.clientAllowWiretapping ?: false)
            .let {
                if(properties.clientSslSecurity == true) it.secure{ it.sslContext(SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE)) }
                else it
            }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    fun intercomProviderBeanPostProcessor(
        @Autowired properties: IntercomPropertiesConfiguration,
        @Autowired @Qualifier(INTERCOM_TCP_CLIENT_BEAN_ID) tcpClient: TcpClient,
        @Autowired intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
        @Autowired intercomReturnBundleSerializer: IntercomReturnBundleSerializer
    ): IntercomProviderBeanPostProcessor =
        IntercomProviderBeanPostProcessor(
            tcpClient = tcpClient,
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