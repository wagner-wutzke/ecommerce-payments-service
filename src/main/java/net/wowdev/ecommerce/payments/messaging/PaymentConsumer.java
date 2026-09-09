package net.wowdev.ecommerce.payments.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationServiceInterface;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationServiceInterface;
import net.wowdev.ecommerce.domain.events.CustomerLoadedEvent;
import net.wowdev.ecommerce.domain.events.InventoryUpdatedEvent;
import net.wowdev.ecommerce.domain.events.InvoiceFailedEvent;
import net.wowdev.ecommerce.domain.events.PaymentMethodLoadedEvent;
import net.wowdev.ecommerce.payments.service.PaymentService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
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

  @KafkaHandler
  public void handle(CustomerLoadedEvent event) {
    log.debug(
        ">> Processing CustomerLoadedEvent sent by {}. Event id: {}",
        event.origin(),
        event.eventId());
    customerReplicationService.replicate(event.customerDTO());
  }

  @KafkaHandler
  public void handle(PaymentMethodLoadedEvent event) {
    log.debug(
        ">> Processing PaymentMethodLoadedEvent sent by {}. Event id: {}",
        event.origin(),
        event.eventId());
    paymentMethodReplicationService.replicate(event.paymentMethodDTO());
  }

  @KafkaHandler
  public void handle(InventoryUpdatedEvent event) {
    log.debug(
        ">> Processing InventoryUpdatedEvent sent by {}. Event id: {}",
        event.origin(),
        event.origin());
    paymentService.process(event.orderDTO());
  }

  @KafkaHandler
  public void handle(InvoiceFailedEvent event) {
    log.debug(
        ">> Processing InvoiceFailedEvent sent by {}. Event id: {}",
        event.origin(),
        event.origin());
    paymentService.compensate(event.ordetDTO(), event.reason());
  }

  @KafkaHandler(isDefault = true)
  public void handleUnknown(Object event) {
    //log.debug(">> Received an unmapped event of type {}", event.getClass().getSimpleName());
  }
}
