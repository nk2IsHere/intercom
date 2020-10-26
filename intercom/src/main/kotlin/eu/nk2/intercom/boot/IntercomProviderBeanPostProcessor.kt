package eu.nk2.intercom.boot

import eu.nk2.intercom.IntercomError
import eu.nk2.intercom.IntercomException
import eu.nk2.intercom.IntercomMethodBundle
import eu.nk2.intercom.IntercomReturnBundle
import eu.nk2.intercom.api.IntercomMethodBundleSerializer
import eu.nk2.intercom.api.IntercomReturnBundleSerializer
import eu.nk2.intercom.api.ProvideIntercom
import eu.nk2.intercom.utils.firstMapWith
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.util.ReflectionUtils
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kafka.receiver.KafkaReceiver
import reactor.kafka.receiver.ReceiverOptions
import reactor.kafka.receiver.ReceiverRecord
import reactor.kafka.sender.KafkaSender
import reactor.kafka.sender.SenderOptions
import reactor.kafka.sender.SenderRecord
import reactor.kotlin.core.publisher.toMono
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@Component
class IntercomProviderBeanPostProcessor(
    private val kafkaStreamProperties: Map<String, Any>,
    private val intercomMethodBundleSerializer: IntercomMethodBundleSerializer,
    private val intercomReturnBundleSerializer: IntercomReturnBundleSerializer
): BeanPostProcessor {
    private val logger = LoggerFactory.getLogger(IntercomProviderBeanPostProcessor::class.java)

    private lateinit var kafkaSender: KafkaSender<String, ByteArray>
    private val kafkaReceivers: MutableMap<String, Flux<ConsumerRecord<String, ByteArray>>> = ConcurrentHashMap()

    @EventListener fun init(event: ContextRefreshedEvent) {
        kafkaSender = KafkaSender.create(SenderOptions.create<String, ByteArray>(kafkaStreamProperties))
    }

    @EventListener fun dispose(event: ContextClosedEvent) {
        kafkaSender.close()
    }

    fun bootstrapKafkaRequestReceiver(id: String) =
        KafkaReceiver.create(
            ReceiverOptions.create<String, ByteArray>(kafkaStreamProperties + (ConsumerConfig.GROUP_ID_CONFIG to UUID.randomUUID().toString()))
                .subscription(listOf("${kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PREFIX_KEY]}_response_${id.hashCode()}"))
                .addAssignListener { partitions -> logger.debug("Provider $id onPartitionsAssigned {}", partitions) }
                .addRevokeListener { partitions -> logger.debug("Provider $id onPartitionsRevoked {}", partitions) }
                .commitInterval(Duration.ZERO)
        ).receiveAutoAck()
            .concatMap { it }
            .publish()
            .autoConnect(0)

    fun makeIntercomRequest(id: String, method: Method, args: Array<Any>): Mono<Any?> {
        return kafkaSender.send(Mono.just(UUID.randomUUID().toString() to id.hashCode())
            .map { (requestId, publisherId) -> SenderRecord.create(
                ProducerRecord(
                    "${kafkaStreamProperties[INTERCOM_KAFKA_TOPIC_PREFIX_KEY]}_request_$publisherId",
                    requestId,
                    intercomMethodBundleSerializer.serialize(IntercomMethodBundle(
                        publisherId = publisherId,
                        methodId = method.name.hashCode() xor method.parameters.map { it.type.name }.hashCode(),
                        parameters = args
                    ))
                ),
                requestId
            ) }
        ).flatMap { kafkaReceivers.getValue(id).firstMapWith(it.correlationMetadata()) }
            .map { (requestId, it) -> requestId to it }
            .filter { (requestId, record) -> requestId == record.key() }
            .map { (_, record) -> record }
//            .flatMap { (_, record) -> Mono.fromCallable {
//                record.receiverOffset().acknowledge()
//                record
//            } }
            .map { it.value() }
            .map { Optional.ofNullable(intercomReturnBundleSerializer.deserialize<Any>(it)) }
            .filter { it.isPresent }
            .map { it.get() }
            .defaultIfEmpty(IntercomReturnBundle(error = IntercomError.INTERNAL_ERROR, data = null))
            .doOnNext { if(it.error != null) throw IntercomException(it.error) }
            .flatMap { it.data?.toMono() ?: Mono.empty() }
            .next()
    }

    fun mapProviderField(bean: Any, beanName: String, field: Field) {
        logger.debug("Mapping $beanName's provider field to proxy")
        val id = field.getAnnotation(ProvideIntercom::class.java)?.id
            ?: error("id is required in annotation @ProvideIntercom")

        kafkaReceivers.putIfAbsent(id, bootstrapKafkaRequestReceiver(id))
        ReflectionUtils.makeAccessible(field)
        field.set(bean, Proxy.newProxyInstance(bean.javaClass.classLoader, arrayOf(field.type)) { _, method, args ->
            return@newProxyInstance makeIntercomRequest(id, method, args)
                .let {
                    when(method.returnType) {
                        Mono::class.java -> it
                        Flux::class.java -> it.flatMapMany { Flux.fromIterable(it as List<*>) }
                        else -> error("Unknown error")
                    }
                }
        })
    }

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any? {
        return bean.apply {
            ReflectionUtils.doWithFields(
                bean.javaClass,
                { field -> mapProviderField(bean, beanName, field) },
                { field -> field.isAnnotationPresent(ProvideIntercom::class.java) }
            )
        }
    }

    override fun postProcessAfterInitialization(bean: Any, beanName: String): Any? {
        return super.postProcessAfterInitialization(bean, beanName)
    }
}