package eu.nk2.intercom.boot.stream

import eu.nk2.intercom.*
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomProviderResolutionRegistry
import eu.nk2.intercom.api.IntercomProviderStreamFactory
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.boot.resolution.DefaultIntercomProviderResolutionEntry
import reactor.core.publisher.Mono
import reactor.netty.tcp.TcpClient
import java.time.Duration
import java.util.*

class DefaultIntercomProviderStreamFactory(
    private val tcpClient: TcpClient,
    providerResolutionRegistry: IntercomProviderResolutionRegistry<DefaultIntercomProviderResolutionEntry>,
    methodBundleSerializer: IntercomMethodBundleSerializer,
    returnBundleSerializer: IntercomReturnBundleSerializer
): IntercomProviderStreamFactory(
    providerResolutionRegistry = providerResolutionRegistry,
    methodBundleSerializer = methodBundleSerializer,
    returnBundleSerializer = returnBundleSerializer
) {

    override fun buildRequest(
        publisherRegistryId: String,
        publisherId: Int,
        methodId: Int,
        args: Array<Any>
    ): Mono<Any?> =
        providerResolutionRegistry.get(publisherRegistryId)
            ?.let { resolutionEntry ->
                val castResolutionEntry = resolutionEntry as DefaultIntercomProviderResolutionEntry
                tcpClient
                    .host(castResolutionEntry.host)
                    .port(castResolutionEntry.port)
                    .connect()
                    .flatMap { connection ->
                        connection
                            .outbound()
                            .sendByteArray(Mono.fromCallable {
                                methodBundleSerializer
                                    .serialize(
                                        IntercomMethodBundle(
                                            publisherId = publisherId,
                                            methodId =  methodId,
                                            parameters = args
                                        )
                                    )
                            })
                            .then()
                            .thenReturn(connection)
                    }
                    .flatMap { connection ->
                        connection
                            .inbound()
                            .receive()
                            .asByteArray()
                            .map { Optional.ofNullable(returnBundleSerializer.deserialize(it)) }
                            .filter { it.isPresent }
                            .map { it.get() }
                            .defaultIfEmpty(IntercomReturnBundle(error = ClientNoDataIntercomError, data = null))
                            .flatMap { when {
                                it.data != null -> Mono.just(it.data)
                                else -> Mono.error(IntercomException(it.error ?: UnreachableIntercomError))
                            } }
                            .next()
                            .doOnNext { connection.disposeNow(Duration.ZERO) }
                    }
            }
            ?: Mono.error(IntercomException(NoProviderResolutionEntry(publisherRegistryId)))

}
