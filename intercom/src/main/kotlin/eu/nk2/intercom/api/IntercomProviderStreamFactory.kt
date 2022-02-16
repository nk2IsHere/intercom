package eu.nk2.intercom.api

import reactor.core.publisher.Mono

abstract class IntercomProviderStreamFactory(
    protected val providerResolutionRegistry: IntercomProviderResolutionRegistry<out IntercomProviderResolutionEntry>,
    protected val methodBundleSerializer: IntercomMethodBundleSerializer,
    protected val returnBundleSerializer: IntercomReturnBundleSerializer
) {

    open fun initialize() = Unit
    open fun dispose() = Unit
    abstract fun buildRequest(
        publisherRegistryId: String,
        publisherId: Int,
        methodId: Int,
        args: Array<Any>
    ): Mono<Any?>
}
