package net.wowdev.ecommerce.payments.config;

import net.wowdev.ecommerce.payments.messaging.PaymentChangeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConfigTest {
    @Test
    void createsProducerConsumerTemplateAndErrorHandler() {
        final KafkaConfig config = new KafkaConfig();
        ReflectionTestUtils.setField(config, "brokers", "localhost:9092");
        ReflectionTestUtils.setField(config, "groupId", "payments");
        ReflectionTestUtils.setField(config, "acks", "all");
        ReflectionTestUtils.setField(config, "retries", 5);
        ReflectionTestUtils.setField(config, "idempotence", true);
        ReflectionTestUtils.setField(config, "inFlight", 5);
        ReflectionTestUtils.setField(config, "trustedPackages", "net.wowdev.ecommerce.domain.dto");
        final ProducerFactory<String, PaymentChangeEvent> producer = config.paymentProducerFactory();
        final ConsumerFactory<String, PaymentChangeEvent> consumer = config.paymentConsumerFactory();
        final KafkaTemplate<String, PaymentChangeEvent> template = config.paymentKafkaTemplate(producer);
        final ConcurrentKafkaListenerContainerFactory<String, PaymentChangeEvent> factory =
                config.kafkaListenerContainerFactory(consumer, template);
        assertThat(producer).isNotNull();
        assertThat(factory.getConsumerFactory()).isSameAs(consumer);
    }
}
