package net.wowdev.ecommerce.payments.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;

import java.time.Instant;
import java.util.UUID;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationService;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationService;
import net.wowdev.ecommerce.domain.dto.CustomerDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentMethodDTO;
import net.wowdev.ecommerce.domain.events.CustomerReplicationCompleted;
import net.wowdev.ecommerce.domain.events.InventoryCompleted;
import net.wowdev.ecommerce.domain.events.InvoiceFailed;
import net.wowdev.ecommerce.domain.events.PaymentMethodReplicationCompleted;
import net.wowdev.ecommerce.payments.service.PaymentService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;

class PaymentConsumerTest {
  @Test
  void delegatesSupportedEventsAndIgnoresUnknownEvents() {
    final CustomerReplicationService customers = mock(CustomerReplicationService.class);
    final PaymentMethodReplicationService methods = mock(PaymentMethodReplicationService.class);
    final PaymentService payments = mock(PaymentService.class);
    final CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
    final CircuitBreaker breaker = mock(CircuitBreaker.class);
    when(factory.create("ecommerceCircuitBreakerGateway")).thenReturn(breaker);
    when(breaker.run(any(), any())).thenAnswer(invocation -> {
      final Supplier<?> operation = invocation.getArgument(0);
      return operation.get();
    });
    final PaymentConsumer consumer = new PaymentConsumer(customers, methods, payments, factory);
    final OrderDTO order = new OrderDTO();

    consumer.handle(
        new CustomerReplicationCompleted(
            UUID.randomUUID(),
            "customer-tx",
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

  @Test
  void delegatesInvoiceFailuresToPaymentCompensation() {
    final CustomerReplicationService customers = mock(CustomerReplicationService.class);
    final PaymentMethodReplicationService methods = mock(PaymentMethodReplicationService.class);
    final PaymentService payments = mock(PaymentService.class);
    final CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
    final CircuitBreaker breaker = mock(CircuitBreaker.class);
    when(factory.create("ecommerceCircuitBreakerGateway")).thenReturn(breaker);
    when(breaker.run(any(), any())).thenAnswer(invocation -> {
      final Supplier<?> operation = invocation.getArgument(0);
      return operation.get();
    });
    final PaymentConsumer consumer = new PaymentConsumer(customers, methods, payments, factory);
    final OrderDTO order = new OrderDTO();
    final String reason = "invoice generation failed";

    consumer.handle(
        new InvoiceFailed(
            UUID.randomUUID(),
            "invoice-tx",
            order,
            Instant.now(),
            reason,
            PaymentService.ORIGIN_SERVICE));

    verify(payments).compensate(order, reason);
  }

  @Test
  void failsInventoryHandlingWhenCircuitBreakerUsesFallback() {
    final CustomerReplicationService customers = mock(CustomerReplicationService.class);
    final PaymentMethodReplicationService methods = mock(PaymentMethodReplicationService.class);
    final PaymentService payments = mock(PaymentService.class);
    final CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
    final CircuitBreaker breaker = mock(CircuitBreaker.class);
    when(factory.create("ecommerceCircuitBreakerGateway")).thenReturn(breaker);
    when(breaker.run(any(), any())).thenAnswer(invocation -> {
      final java.util.function.Function<Throwable, ?> fallback = invocation.getArgument(1);
      return fallback.apply(new IllegalStateException("gateway unavailable"));
    });
    final PaymentConsumer consumer = new PaymentConsumer(customers, methods, payments, factory);
    final OrderDTO order = new OrderDTO();
    final InventoryCompleted event = new InventoryCompleted(
        UUID.randomUUID(), "inventory-tx", order, Instant.now(), PaymentService.ORIGIN_SERVICE);

    final RuntimeException exception = org.junit.jupiter.api.Assertions.assertThrows(
        RuntimeException.class, () -> consumer.handle(event));

    org.junit.jupiter.api.Assertions.assertEquals(
        ">> Payment pipeline temporarily unavailable. Retrying via broker...", exception.getMessage());
    org.junit.jupiter.api.Assertions.assertInstanceOf(IllegalStateException.class, exception.getCause());
  }

  @Test
  void identifiesOpenCircuitBreakerFailureSeparately() {
    final CustomerReplicationService customers = mock(CustomerReplicationService.class);
    final PaymentMethodReplicationService methods = mock(PaymentMethodReplicationService.class);
    final PaymentService payments = mock(PaymentService.class);
    final CircuitBreakerFactory<?, ?> factory = mock(CircuitBreakerFactory.class);
    final CircuitBreaker breaker = mock(CircuitBreaker.class);
    when(factory.create("ecommerceCircuitBreakerGateway")).thenReturn(breaker);
    when(breaker.run(any(), any())).thenAnswer(invocation -> {
      final java.util.function.Function<Throwable, ?> fallback = invocation.getArgument(1);
      final io.github.resilience4j.circuitbreaker.CircuitBreaker resilience4jBreaker =
          io.github.resilience4j.circuitbreaker.CircuitBreaker.ofDefaults("test");
      return fallback.apply(
          CallNotPermittedException.createCallNotPermittedException(resilience4jBreaker));
    });
    final PaymentConsumer consumer = new PaymentConsumer(customers, methods, payments, factory);
    final InventoryCompleted event = new InventoryCompleted(
        UUID.randomUUID(), "inventory-tx", new OrderDTO(), Instant.now(), PaymentService.ORIGIN_SERVICE);

    final CircuitBreakerOpenException exception = org.junit.jupiter.api.Assertions.assertThrows(
        CircuitBreakerOpenException.class, () -> consumer.handle(event));

    org.junit.jupiter.api.Assertions.assertInstanceOf(
        CallNotPermittedException.class, exception.getCause());
  }
}
