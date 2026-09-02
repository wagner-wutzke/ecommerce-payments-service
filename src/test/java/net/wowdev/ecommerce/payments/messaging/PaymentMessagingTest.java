package net.wowdev.ecommerce.payments.messaging;

import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationService;
import net.wowdev.ecommerce.datareplication.service.OrderReplicationService;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationService;
import net.wowdev.ecommerce.domain.events.*;
import net.wowdev.ecommerce.payments.TestFixtures;
import net.wowdev.ecommerce.payments.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;

class PaymentMessagingTest {
  @Test
  void delegatesIncomingEvents() {
    final CustomerReplicationService customers = mock(CustomerReplicationService.class);
    final OrderReplicationService orders = mock(OrderReplicationService.class);
    final PaymentMethodReplicationService methods = mock(PaymentMethodReplicationService.class);
    final PaymentService paymentService = mock(PaymentService.class);
    final PaymentConsumer consumer =
        new PaymentConsumer(customers, orders, methods, paymentService);
    consumer.handleCustomerLoaded(
        new CustomerLoadedEvent(
            UUID.randomUUID(),
            "tx",
            new net.wowdev.ecommerce.domain.dto.CustomerDTO(),
            Instant.now(),
            PaymentProducer.ORIGIN_SERVICE));
    consumer.handleOrderCreated(
        new OrderCreatedEvent(
            UUID.randomUUID(),
            "tx",
            new net.wowdev.ecommerce.domain.dto.OrderDTO(),
            Instant.now(),
            PaymentProducer.ORIGIN_SERVICE));
    consumer.handlePaymentMethodLoaded(
        new PaymentMethodLoadedEvent(
            UUID.randomUUID(),
            "tx",
            new net.wowdev.ecommerce.domain.dto.PaymentMethodDTO(),
            Instant.now(),
            PaymentProducer.ORIGIN_SERVICE));
    verify(customers).replicate(any());
    verify(orders).replicate(any());
    verify(methods).replicate(any());
  }

  @Test
  void publishesCompletedPaymentAfterCommit() {
    final KafkaTemplate<String, Object> template = mock(KafkaTemplate.class);
    final PaymentProducer producer = new PaymentProducer(template, "payment-events");
    final var payment = TestFixtures.paymentDto();
    producer.publish(
        new PaymentCompletedEvent(
            UUID.randomUUID(), "tx", payment, Instant.now(), PaymentProducer.ORIGIN_SERVICE));
    verify(template).send(eq("payment-events"), eq(payment.getId().toString()), any());
  }
}
