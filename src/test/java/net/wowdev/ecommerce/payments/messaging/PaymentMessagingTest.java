package net.wowdev.ecommerce.payments.messaging;

import net.wowdev.ecommerce.payments.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentMessagingTest {
    @Test
    void producerPublishesAfterCommitWithPaymentIdKey() {
        final KafkaTemplate<String, PaymentChangeEvent> template = mock(KafkaTemplate.class);
        final PaymentProducer producer = new PaymentProducer(template, "payments-change-topic");
        final PaymentChangeEvent event = new PaymentChangeEvent("CREATE", TestFixtures.paymentDto());
        producer.publishAfterCommit(event);
        verify(template).send("payments-change-topic", event.payload().getId().toString(), event);
    }

    @Test
    void consumerAcceptsEventWithoutBroker() {
        new PaymentConsumer().consume(new PaymentChangeEvent("UPDATE", TestFixtures.paymentDto()));
    }
}
