package net.wowdev.ecommerce.payments.messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.CustomerLoadedEvent;
import net.wowdev.ecommerce.domain.events.OrderCreatedEvent;
import net.wowdev.ecommerce.domain.events.OrderProcessingStartedEvent;
import net.wowdev.ecommerce.domain.events.PaymentMethodLoadedEvent;
import net.wowdev.ecommerce.payments.service.CustomerReplicationService;
import net.wowdev.ecommerce.payments.service.OrderReplicationService;
import net.wowdev.ecommerce.payments.service.PaymentMethodReplicationService;
import net.wowdev.ecommerce.payments.service.PaymentService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@KafkaListener(
    groupId = "${spring.kafka.consumer.group-id}",
    topics = { "${app.kafka.customer-events-topic}", "${app.kafka.order-events-topic}" },
    containerFactory = "kafkaListenerContainerFactory")
public class PaymentConsumer {

  private final CustomerReplicationService customerDataReplicationService;
  private final OrderReplicationService orderReplicationService;
  private final PaymentMethodReplicationService paymentMethodReplicationService;
  private final PaymentService paymentService;

  @KafkaHandler
  public void handleOrderCreated(OrderCreatedEvent event) {
    log.debug(">>>> Processing OrderCreatedEvent: {}", event.eventId());
    orderReplicationService.replicate(event.orderDTO());
  }

  @KafkaHandler
  public void handleCustomerLoaded(CustomerLoadedEvent event) {
    log.debug(">>>> Processing CustomerLoadedEvent: {}", event.eventId());
    customerDataReplicationService.replicate(event.customerDTO());
  }

  @KafkaHandler
  public void handlePaymentMethodLoaded(PaymentMethodLoadedEvent event) {
    log.debug(">>>> Processing PaymentMethodLoadedEvent: {}", event.eventId());
    paymentMethodReplicationService.replicate(event.paymentMethodDTO());
  }

  @KafkaHandler
  public void handleOrderProcessingStarted(OrderProcessingStartedEvent event) {
    log.debug(">>>> Processing OrderProcessingStartedEvent: {}", event.eventId());
    paymentService.process(event.orderDTO());
  }

}
