package net.wowdev.ecommerce.payments.config;

import net.wowdev.ecommerce.payments.messaging.PaymentChangeEvent;
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
    @Value("${spring.kafka.bootstrap-servers}")
    private String brokers;
    @Value("${app.kafka.consumer-group}")
    private String groupId;
    @Value("${spring.kafka.producer.acks:all}")
    private String acks;
    @Value("${spring.kafka.producer.retries:5}")
    private Integer retries;
    @Value("${spring.kafka.producer.properties.enable.idempotence:true}")
    private boolean idempotence;
    @Value("${spring.kafka.producer.properties.max.in.flight.requests.per.connection:5}")
    private Integer inFlight;
    @Value("${spring.kafka.consumer.properties.spring.json.trusted.packages:net.wowdev.ecommerce.domain.dto}")
    private String trustedPackages;

    @Bean
    public ProducerFactory<String, PaymentChangeEvent> paymentProducerFactory() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        properties.put(ProducerConfig.ACKS_CONFIG, acks);
        properties.put(ProducerConfig.RETRIES_CONFIG, retries);
        properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, idempotence);
        properties.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, inFlight);
        return new DefaultKafkaProducerFactory<>(properties);
    }

    @Bean
    public KafkaTemplate<String, PaymentChangeEvent> paymentKafkaTemplate(
            final ProducerFactory<String, PaymentChangeEvent> factory) {
        return new KafkaTemplate<>(factory);
    }

    @Bean
    public ConsumerFactory<String, PaymentChangeEvent> paymentConsumerFactory() {
        final Map<String, Object> properties = new HashMap<>();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers);
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
        properties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        return new DefaultKafkaConsumerFactory<>(properties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, PaymentChangeEvent> kafkaListenerContainerFactory(
            final ConsumerFactory<String, PaymentChangeEvent> consumerFactory,
            final KafkaTemplate<String, PaymentChangeEvent> template) {
        final ConcurrentKafkaListenerContainerFactory<String, PaymentChangeEvent> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(new DefaultErrorHandler(new DeadLetterPublishingRecoverer(template),
                new FixedBackOff(2_000L, retries)));
        return factory;
    }
}
