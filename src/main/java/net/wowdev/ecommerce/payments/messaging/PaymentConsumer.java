package net.wowdev.ecommerce.payments.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationServiceInterface;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationServiceInterface;
import net.wowdev.ecommerce.domain.events.CustomerLoadedEvent;
import net.wowdev.ecommerce.domain.events.InventoryUpdatedEvent;
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
      "${app.kafka.inventory-topic}"
    },
    containerFactory = "kafkaListenerContainerFactory")
public class PaymentConsumer {

  private final CustomerReplicationServiceInterface customerReplicationService;
  private final PaymentMethodReplicationServiceInterface paymentMethodReplicationService;
  private final PaymentService paymentService;

  @KafkaHandler
  public void handle(CustomerLoadedEvent event) {
    log.debug(">>>> Processing CustomerLoadedEvent: {}", event.eventId());
    customerReplicationService.replicate(event.customerDTO());
  }

  @KafkaHandler
  public void handle(PaymentMethodLoadedEvent event) {
    log.debug(">>>> Processing PaymentMethodLoadedEvent: {}", event.eventId());
    paymentMethodReplicationService.replicate(event.paymentMethodDTO());
  }

  @KafkaHandler
  public void handle(InventoryUpdatedEvent event) {
    log.debug(">>>> Processing InventoryUpdatedEvent: {}", event.eventId());
    paymentService.process(event.orderDTO());
  }
}
