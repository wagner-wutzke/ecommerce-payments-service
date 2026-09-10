package net.wowdev.ecommerce.payments.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationService;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationService;
import net.wowdev.ecommerce.domain.dto.CustomerDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentMethodDTO;
import net.wowdev.ecommerce.domain.events.CustomerReplicationCompleted;
import net.wowdev.ecommerce.domain.events.InventoryCompleted;
import net.wowdev.ecommerce.domain.events.PaymentMethodReplicationCompleted;
import net.wowdev.ecommerce.payments.service.PaymentService;
import org.junit.jupiter.api.Test;

class PaymentConsumerTest {
  @Test
  void delegatesSupportedEventsAndIgnoresUnknownEvents() {
    final CustomerReplicationService customers = mock(CustomerReplicationService.class);
    final PaymentMethodReplicationService methods = mock(PaymentMethodReplicationService.class);
    final PaymentService payments = mock(PaymentService.class);
    final PaymentConsumer consumer = new PaymentConsumer(customers, methods, payments);
    final OrderDTO order = new OrderDTO();

    consumer.handle(
        new CustomerReplicationCompleted(
            UUID.randomUUID(),
            "customer-tx",
            new OrderDTO(),
            new CustomerDTO(),
            Instant.now(),
            PaymentService.ORIGIN_SERVICE));

    consumer.handle(
        new PaymentMethodReplicationCompleted(
            UUID.randomUUID(),
            "payment-method-tx",
            new PaymentMethodDTO(),
            Instant.now(),
            PaymentService.ORIGIN_SERVICE));

    consumer.handle(
        new InventoryCompleted(
            UUID.randomUUID(),
            "inventory-tx",
            order,
            Instant.now(),
            PaymentService.ORIGIN_SERVICE));
    consumer.handleUnknown("unmapped event");

    verify(customers).replicate(any());
    verify(methods).replicate(any());
    verify(payments).process(order);
  }
}
