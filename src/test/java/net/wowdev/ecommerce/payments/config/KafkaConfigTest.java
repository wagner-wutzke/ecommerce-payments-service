package net.wowdev.ecommerce.payments.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.util.ReflectionTestUtils;

class KafkaConfigTest {

  private static KafkaConfig configuredKafkaConfig() {
    final KafkaConfig config = new KafkaConfig();
    ReflectionTestUtils.setField(config, "consumerGroup", "payments-events");
    ReflectionTestUtils.setField(config, "bootstrapServers", "localhost:9092");
    ReflectionTestUtils.setField(config, "acks", "all");
    ReflectionTestUtils.setField(config, "deliveryTimeout", "30000");
    ReflectionTestUtils.setField(config, "linger", "0");
    ReflectionTestUtils.setField(config, "requestTimeout", "10000");
    ReflectionTestUtils.setField(config, "idempotence", true);
    ReflectionTestUtils.setField(config, "retries", 3);
    ReflectionTestUtils.setField(config, "maxRequestsInFlight", 5);
    ReflectionTestUtils.setField(config, "trustedPackages", "net.wowdev.ecommerce.domain.events");
    return config;
  }

  private static void assertProducerConfiguration(
      final ProducerFactory<String, Object> producerFactory) {
    final MapAssertions properties =
        new MapAssertions(
            producerFactory
                .getConfigurationProperties());
    properties.contains(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    properties.contains(ProducerConfig.ACKS_CONFIG, "all");
    properties.contains(ProducerConfig.RETRIES_CONFIG, 3);
    properties.contains(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
    properties.contains(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
    properties.contains(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, "30000");
    properties.contains(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "10000");
    properties.contains(ProducerConfig.LINGER_MS_CONFIG, "0");
    properties.contains(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    properties.contains(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
  }

  private static void assertConsumerConfiguration(
      final ConsumerFactory<String, Object> consumerFactory) {
    final MapAssertions properties =
        new MapAssertions(
            consumerFactory
                .getConfigurationProperties());
    properties.contains(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
    properties.contains(ConsumerConfig.GROUP_ID_CONFIG, "payments-events");
    properties.contains(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    properties.contains(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JacksonJsonDeserializer.class);
    properties.contains(
        JacksonJsonDeserializer.TRUSTED_PACKAGES, "net.wowdev.ecommerce.domain.events");
  }

  @Test
  void createsConfiguredKafkaBeans() {
    final KafkaConfig config = configuredKafkaConfig();

    final ProducerFactory<String, Object> producerFactory = config.producerFactory();
    final ConsumerFactory<String, Object> consumerFactory = config.consumerFactory();
    final KafkaTemplate<String, Object> kafkaTemplate = config.kafkaTemplate(producerFactory);
    final ConcurrentKafkaListenerContainerFactory<String, Object> listenerFactory =
        config.kafkaListenerContainerFactory(consumerFactory, kafkaTemplate);

    assertProducerConfiguration(producerFactory);
    assertConsumerConfiguration(consumerFactory);
    assertThat(kafkaTemplate.getProducerFactory()).isSameAs(producerFactory);
    assertThat(listenerFactory.getConsumerFactory()).isSameAs(consumerFactory);
    assertThat(ReflectionTestUtils.getField(listenerFactory, "concurrency")).isEqualTo(3);
    assertThat(ReflectionTestUtils.getField(listenerFactory, "commonErrorHandler"))
        .isInstanceOf(DefaultErrorHandler.class);
    assertThat(config.multiTypeConverter()).isNotNull();
  }

  private record MapAssertions(java.util.Map<String, Object> values) {
    void contains(final String key, final Object value) {
      assertThat(values).containsEntry(key, value);
    }
  }
}
