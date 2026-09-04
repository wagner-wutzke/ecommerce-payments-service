package net.wowdev.ecommerce.payments.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.events.PaymentCompletedEvent;
import net.wowdev.ecommerce.domain.events.PaymentFailedEvent;
import net.wowdev.ecommerce.payments.TestFixtures;
import net.wowdev.ecommerce.payments.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class PaymentProducerTest {
  @Test
  void publishesCompletedPaymentUsingPaymentIdAsKey() {
    final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    final PaymentProducer producer = new PaymentProducer(template, "payment-events");
    final PaymentDTO payment = TestFixtures.paymentDto();

    producer.publish(
        new PaymentCompletedEvent(
            UUID.randomUUID(),
            "payment-tx",
            new OrderDTO(),
            payment,
            Instant.now(),
            PaymentService.ORIGIN_SERVICE));

    verify(template).send(eq("payment-events"), eq(payment.getId().toString()), any());
  }

  @Test
  void publishesFailedPaymentUsingOrderIdAsKey() {
    final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    final PaymentProducer producer = new PaymentProducer(template, "payment-events");
    final OrderDTO order = new OrderDTO();
    order.setId(UUID.fromString("55555555-5555-5555-5555-555555555555"));

    producer.publish(
        new PaymentFailedEvent(
            UUID.randomUUID(),
            "payment-tx",
            order,
            "declined",
            Instant.now(),
            PaymentService.ORIGIN_SERVICE));

    verify(template).send(eq("payment-events"), eq(order.getId().toString()), any());
  }
}
