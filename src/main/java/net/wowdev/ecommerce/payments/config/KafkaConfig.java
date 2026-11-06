package net.wowdev.ecommerce.payments.config;

import net.wowdev.ecommerce.payments.commons.Utils;
import net.wowdev.ecommerce.payments.messaging.CircuitBreakerOpenException;
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
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.BackOff;

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

    @Value("${app.kafka.retry.max-retries:3}")
    private Integer consumerMaxRetries;

    @Value("${app.kafka.retry.multiplier:2.0}")
    private Double consumerRetryMultiplier;

    @Value("${app.kafka.retry.max-interval-ms:30000}")
    private Long consumerRetryMaxInterval;

    @Value("${app.kafka.retry.circuit-breaker-open.initial-interval-ms:16000}")
    private Long circuitBreakerOpenRetryInitialInterval;

    @Value("${app.kafka.retry.circuit-breaker-open.max-interval-ms:60000}")
    private Long circuitBreakerOpenRetryMaxInterval;

    @Value("${spring.kafka.consumer.properties.retry.backoff.ms}")
    private Integer retryBackoff;

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
        config.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoff);
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
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, retryBackoff);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory(
            final ConsumerFactory<String, Object> orderConsumerFactory,
            final KafkaTemplate<String, Object> kafkaTemplate) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, Object>();
        factory.setConsumerFactory(orderConsumerFactory);
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        DefaultErrorHandler defaultErrorHandler =
                new DefaultErrorHandler(
                        new DeadLetterPublishingRecoverer(kafkaTemplate),
                        retryBackOff(retryBackoff, consumerRetryMaxInterval));
        BackOff circuitBreakerOpenBackOff = retryBackOff(
                circuitBreakerOpenRetryInitialInterval,
                circuitBreakerOpenRetryMaxInterval);
        // The first failed call can open the breaker, so the same record may move from an
        // ordinary processing exception to CircuitBreakerOpenException on its next delivery.
        defaultErrorHandler.setResetStateOnExceptionChange(true);
        defaultErrorHandler.setBackOffFunction((record, exception) ->
                Utils.hasCause(exception, CircuitBreakerOpenException.class)
                        ? circuitBreakerOpenBackOff
                        : retryBackOff(retryBackoff, consumerRetryMaxInterval));
        factory.setCommonErrorHandler(defaultErrorHandler);
        return factory;
    }

    private BackOff retryBackOff(long initialInterval, long maxInterval) {
        ExponentialBackOffWithMaxRetries backOff =
                new ExponentialBackOffWithMaxRetries(consumerMaxRetries);
        backOff.setInitialInterval(initialInterval);
        backOff.setMultiplier(consumerRetryMultiplier);
        backOff.setMaxInterval(maxInterval);
        return backOff;
    }

}
