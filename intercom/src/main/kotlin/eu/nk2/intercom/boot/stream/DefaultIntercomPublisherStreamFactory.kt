package eu.nk2.intercom.boot.stream

import eu.nk2.intercom.IntercomException
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.NoDataIntercomError
import eu.nk2.intercom.api.*
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.netty.DisposableServer
import reactor.netty.tcp.TcpServer
import java.time.Duration
import java.util.*

class DefaultIntercomPublisherStreamFactory(
    private val tcpServer: TcpServer,
    methodBundleSerializer: IntercomMethodBundleSerializer,
    returnBundleSerializer: IntercomReturnBundleSerializer
): IntercomPublisherStreamFactory(
    methodBundleSerializer = methodBundleSerializer,
    returnBundleSerializer = returnBundleSerializer
) {

    private val log = LoggerFactory.getLogger(DefaultIntercomPublisherStreamFactory::class.java)

    private var tcpServerDisposable: DisposableServer? = null

    override fun initializeStream(
        publishers: Set<IntercomPublisher>,
        responseFactory: IntercomPublisherResponseFactory
    ) {
        try {
            tcpServerDisposable = tcpServer
                .doOnConnection {
                    log.debug("Connected intercom client: ${it.address()}")
                }
                .handle { input, output ->
                    output.sendByteArray(
                        input.receive().asByteArray()
                            .map { Optional.ofNullable(methodBundleSerializer.deserialize(it)) }
                            .filter { it.isPresent }
                            .map { it.get() }
                            .doOnNext { log.debug("Received from intercom client ${it.publisherId}.${it.methodId}()") }
                            .flatMap { responseFactory.buildResponse(it.publisherId, it.methodId, it.parameters) }
                            .map { IntercomReturnBundle(error = null, data = it) }
                            .onErrorResume(IntercomException::class.java) { Mono.just(IntercomReturnBundle(error = it.error, data = null)) }
                            .defaultIfEmpty(IntercomReturnBundle(error = NoDataIntercomError, data = null))
                            .map { returnBundleSerializer.serialize(it) }
                    )
                }
                .bindNow(Duration.ofMillis(1000))
        } catch (e: Exception) {
            log.error("Error occurred while initializing publisher stream, exiting", e)
            throw e
        }
    }

    override fun disposeStream() {
        tcpServerDisposable?.disposeNow()
    }
}
