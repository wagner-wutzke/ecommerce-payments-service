package net.wowdev.ecommerce.payments.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationServiceInterface;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationServiceInterface;
import net.wowdev.ecommerce.domain.events.CustomerReplicationCompleted;
import net.wowdev.ecommerce.domain.events.InventoryCompleted;
import net.wowdev.ecommerce.domain.events.InvoiceFailed;
import net.wowdev.ecommerce.domain.events.PaymentMethodReplicationCompleted;
import net.wowdev.ecommerce.payments.commons.Utils;
import net.wowdev.ecommerce.payments.service.PaymentService;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.cloud.client.circuitbreaker.CircuitBreaker;
import org.springframework.cloud.client.circuitbreaker.CircuitBreakerFactory;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
        id = "${app.kafka.payments-event-listener-id}",
        groupId = "${spring.kafka.consumer.group-id}",
        topics = {
                "${app.kafka.customers-topic}",
                "${app.kafka.inventory-topic}",
                "${app.kafka.invoices-topic}"
        },
        containerFactory = "kafkaListenerContainerFactory")
public class PaymentConsumer {

    private final CustomerReplicationServiceInterface customerReplicationService;
    private final PaymentMethodReplicationServiceInterface paymentMethodReplicationService;
    private final PaymentService paymentService;
    private final CircuitBreakerFactory circuitBreakerFactory;

    @KafkaHandler
    public void handle(CustomerReplicationCompleted event) {
        log.debug(
                ">> Processing CustomerReplicationCompleted event sent by {}. Event id: {}",
                event.origin(),
                event.eventId());
        customerReplicationService.replicate(event.customerDTO());
    }

    @KafkaHandler
    public void handle(PaymentMethodReplicationCompleted event) {
        log.debug(
                ">> Processing PaymentMethodReplicationCompleted event sent by {}. Event id: {}",
                event.origin(),
                event.eventId());
        paymentMethodReplicationService.replicate(event.paymentMethodDTO());
    }

    @KafkaHandler
    public void handle(InventoryCompleted event) {
        log.debug(
                ">> Processing InventoryCompleted event sent by {}. Event id: {}",
                event.origin(),
                event.origin());
        // the same name as defined in the application.yaml
        CircuitBreaker circuitBreaker = circuitBreakerFactory.create("ecommerceCircuitBreakerGateway");

        // Run the call wrapped in the circuit breaker, defining a fallback function
        circuitBreaker.run(
                () -> {
                    paymentService.process(event.orderDTO());
                    return null;
                },
                throwable -> handlePaymentFallback(event, throwable)
        );
    }

    private Void handlePaymentFallback(InventoryCompleted event, Throwable throwable) {
        log.error(">> Circuit breaker fallback activated for order {}. Reason: {}",
                event.orderDTO().getId(),
                throwable.getMessage());

        // Critical Action for EDA:
        // Throw an exception to tell the broker (like Kafka/RabbitMQ) that processing failed.
        // This prevents the message from being acknowledged (ACK'ed) so it can be retried later,
        // or routed to a backing retry/dead-letter topic based on your broker configuration.
        String message = ">> Payment pipeline temporarily unavailable. Retrying via broker...";
        if (Utils.hasCause(throwable, CallNotPermittedException.class)) {
            throw new CircuitBreakerOpenException(message, throwable);
        }
        throw new RuntimeException(message, throwable);
    }


    @KafkaHandler
    public void handle(InvoiceFailed event) {
        log.debug(
                ">> Processing InvoiceFailed event sent by {}. Event id: {}",
                event.origin(),
                event.origin());
        paymentService.compensate(event.ordetDTO(), event.reason());
    }

    @KafkaHandler(isDefault = true)
    public void handleUnknown(Object event) {
        //log.debug(">> Received an unmapped event of type {}", event.getClass().getSimpleName());
    }
}
