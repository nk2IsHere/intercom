package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomException
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.api.PublishIntercom
import eu.nk2.intercom.utils.*
import eu.nk2.intercom.utils.Unsafe.Companion.unsafe
import eu.nk2.intercom.utils.wrapOptional
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.stereotype.Component
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap


@Component class IntercomPublisherBeanPostProcessor(
    private val kafkaStreamProperties: Map<String, Any>,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor, PriorityOrdered {
    private val logger = LoggerFactory.getLogger(IntercomPublisherBeanPostProcessor::class.java)

    private val intercomPublisherAwareBeans = hashMapOf<String, Class<*>>()
    private val intercomPublishers: MutableMap<Int, Pair<Any, Map<Int, Method>>> = ConcurrentHashMap()

    private lateinit var kafkaSender: KafkaSender<String, ByteArray>
    private lateinit var kafkaStreamInstances: MutableList<Disposable>

    fun bootstrapResponseKafkaStream(publisherId: Int): Disposable =
        KafkaReceiver.create(
            ReceiverOptions.create<String, ByteArray>(kafkaStreamProperties + (ConsumerConfig.GROUP_ID_CONFIG to "${kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PREFIX_KEY]}_request_${publisherId}"))
                .subscription(listOf("${kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PREFIX_KEY]}_request_$publisherId"))
                .addAssignListener { partitions -> logger.debug("Receiver $publisherId onPartitionsAssigned $partitions") }
                .addRevokeListener { partitions -> logger.debug("Receiver $publisherId onPartitionsRevoked $partitions") }
                .commitInterval(Duration.ZERO)
        ).receiveAutoAck()
            .concatMap { it }
            .map { record -> record.key() to record.value() }
            .map { (key, value) -> key to Optional.ofNullable(intercomMethodBundleSerializer.deserialize(value)) }
            .filter { (_, value) -> value.isPresent }
            .map { (key, value) -> key to value.get() }
            .doOnNext { (_, value) -> logger.debug("Received from intercom client ${value.publisherId}.${value.methodId}()") }
            .flatMap<Pair<String, IntercomReturnBundle<Any?>>> { (key, value) ->
                run {
                    val publisherDefinition = intercomPublishers[value.publisherId]
                        ?: return@run Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_PUBLISHER))

                    val (publisher, method) = publisherDefinition.first to (publisherDefinition.second[value.methodId]
                        ?: return@run Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_METHOD)))

                    if (method.parameterCount != value.parameters.size)
                        return@run Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_PARAMS))

                    for ((index, parameter) in method.parameters.withIndex())
                        if (ClassUtils.objectiveClass(parameter.type) != ClassUtils.objectiveClass(value.parameters[index].javaClass))
                            return@run Mono.error<Optional<Any>>(IntercomException(IntercomError.BAD_PARAMS))

                    return@run try {
                        when (method.returnType) {
                            Mono::class.java -> (method.invoke(publisher, *value.parameters) as Mono<*>)
                            Flux::class.java -> (method.invoke(publisher, *value.parameters) as Flux<*>).collectList()
                            else -> Mono.error<Any>(IntercomException(IntercomError.INTERNAL_ERROR))
                        }.wrapOptional()
                    } catch (e: Exception) {
                        logger.debug("Error in handling intercom client", e)
                        Mono.error<Optional<Any>>(IntercomException(when (e) {
                            is IllegalArgumentException -> IntercomError.BAD_DATA
                            is InvocationTargetException -> IntercomError.PROVIDER_ERROR
                            else -> IntercomError.INTERNAL_ERROR
                        }))
                    }
                }.map<IntercomReturnBundle<Any?>> { IntercomReturnBundle(error = null, data = it.orNull()) }
                    .onErrorResume(IntercomException::class.java) { Mono.just(IntercomReturnBundle<Any?>(error = it.error, data = null)) }
                    .defaultIfEmpty(IntercomReturnBundle(error = IntercomError.NO_DATA, data = null))
                    .firstMapWith<IntercomReturnBundle<Any?>, String>(key)
            }
            .map { (key, value) -> key to intercomReturnBundleSerializer.serialize(value) }
            .flatMap { (key, value) ->
                kafkaSender.send(Mono.just(SenderRecord.create(
                    ProducerRecord<String, ByteArray>(
                        "${kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PREFIX_KEY]}_response_$publisherId",
                        key, value
                    ),
                    key
                )))
            }
            .subscribe()

    @EventListener fun init(event: ContextRefreshedEvent) {
        AdminClient.create(kafkaStreamProperties)
            .createTopics(
                intercomPublishers.flatMap { (publisherId, _) -> listOf(
                    "${kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PREFIX_KEY]}_request_$publisherId",
                    "${kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PREFIX_KEY]}_response_$publisherId"
                ) }.map {
                    NewTopic(
                        it,
                        kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PARTITION_NUMBER_KEY] as Int,
                        kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_REPLICATION_FACTOR_KEY] as Short
                    )
                }
            )
            .values()
            .forEach { unsafe {
                danger { it.value.get() }
            } }

        kafkaSender = KafkaSender.create(SenderOptions.create<String, ByteArray>(kafkaStreamProperties))
        kafkaStreamInstances = intercomPublishers.map { (publisherId, _) -> bootstrapResponseKafkaStream(publisherId) }
            .toMutableList()
    }

    @EventListener fun dispose(event: ContextClosedEvent) {
        kafkaStreamInstances.forEach { it.dispose() }
        kafkaSender.close()
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? =
        bean.apply {
            if (AnnotationUtils.getAnnotation(bean.javaClass, PublishIntercom::class.java) != null) {
                if(bean.javaClass.constructors.none { it.parameters.isEmpty() })
                    error("Classes that contain methods annotated with @PublishIntercom must contain empty constructor, " +
                        "please look at the implementation of ${bean.javaClass.name}")

                intercomPublisherAwareBeans[beanName] = this.javaClass
            }
        }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        intercomPublisherAwareBeans[beanName]?.let { beanClass ->
            val id = AnnotationUtils.getAnnotation(beanClass, PublishIntercom::class.java)?.id
                ?: error("id is required in annotation @PublishIntercom")

            intercomPublishers[id.hashCode()] = bean to (beanClass.methods
                .asSequence()
                .filter { method -> INTERCOM_ALLOWED_GENERIC_METHOD_RETURN_TYPES.any { it.isAssignableFrom(method.returnType) } }
                .map { it.name.hashCode() xor it.parameters.map { it.type.name }.hashCode() to it }
                .toMap())
            logger.debug("Mapped publisher $id to registry")
        }

        return super.postProcessAfterInitialization(bean, beanName)
    }

    override fun getOrder(): Int = Ordered.LOWEST_PRECEDENCE
}
