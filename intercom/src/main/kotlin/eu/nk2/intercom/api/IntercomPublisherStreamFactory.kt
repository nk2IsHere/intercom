package eu.nk2.intercom.api

abstract class IntercomPublisherStreamFactory(
    protected val methodBundleSerializer: IntercomMethodBundleSerializer,
    protected val returnBundleSerializer: IntercomReturnBundleSerializer
) {

    abstract fun initializeStream(publishers: Set<IntercomPublisher>, responseFactory: IntercomPublisherResponseFactory)
    abstract fun disposeStream()
}
