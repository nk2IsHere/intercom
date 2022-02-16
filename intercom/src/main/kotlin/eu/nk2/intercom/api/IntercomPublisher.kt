package eu.nk2.intercom.api

data class IntercomPublisher(
    val publisherId: Int,
    val methods: Set<IntercomPublisherMethod>
)

data class IntercomPublisherMethod(
    val methodId: Int
)
