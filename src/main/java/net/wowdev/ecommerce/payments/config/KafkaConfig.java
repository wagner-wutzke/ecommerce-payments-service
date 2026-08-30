package net.wowdev.ecommerce.payments.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroup;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.producer.acks:all}")
    private String acks;

    @Value("${spring.kafka.producer.properties.delivery.timeout.ms:30000}")
    private String deliveryTimeout;

    @Value("${spring.kafka.producer.properties.linger.ms:0}")
    private String linger;

    @Value("${spring.kafka.producer.properties.request.timeout.ms:10000}")
    private String requestTimeout;

    @Value("${spring.kafka.producer.properties.enable.idempotence:true}")
    private boolean idempotence;

    @Value("${spring.kafka.producer.retries:3}")
    private Integer retries;

    @Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection:5}")
    private Integer maxRequestsInFlight;

    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages}")
    private String trustedPackages;

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
            final ProducerFactory<String, Object> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        final Map<String, Object> config = new HashMap<>();

        // Mandatory companion of enable.idempotence — Kafka refuses to start the producer
        // Exactly-once-per-partition semantics: the broker deduplicates retried batches
        // using the producer id + sequence number.
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);

        // with acks=0 or acks=1 because it cannot deduplicate without full ISR ack.
        config.put(ProducerConfig.RETRIES_CONFIG, retries);

        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, acks);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, maxRequestsInFlight);
        config.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, deliveryTimeout);
        config.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, requestTimeout);
        config.put(ProducerConfig.LINGER_MS_CONFIG, linger);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        final Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroup);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        props.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            final ConsumerFactory<String, Object> orderConsumerFactory,
            final KafkaTemplate<String, Object> kafkaTemplate) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(orderConsumerFactory);
        factory.setConcurrency(3);

        factory.setCommonErrorHandler(
                new DefaultErrorHandler(new DeadLetterPublishingRecoverer(kafkaTemplate),
                                        new FixedBackOff(2000L, retries)));
        return factory;
    }
}
