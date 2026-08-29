package net.wowdev.ecommerce.payments.messaging;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.CustomerLoadedEvent;
import net.wowdev.ecommerce.domain.events.OrderCreatedEvent;
import net.wowdev.ecommerce.payments.service.CustomerService;
import net.wowdev.ecommerce.payments.service.PaymentService;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
@KafkaListener(
        groupId = "${spring.kafka.consumer.group-id}",
        topics = { "${app.kafka.payment-events-topic}" },
        containerFactory = "kafkaListenerContainerFactory"
)
public class PaymentConsumer {

    private final PaymentService paymentService;
    private final CustomerService customerService;

    @KafkaHandler
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info(">>>> Processing CreatedOrderEvent: {}", event);
    }

    @KafkaHandler
    public void handleCustomerLoaded(CustomerLoadedEvent event) {
        log.info(">>>> Processing CustomerLoadedEvent: {}", event);
        customerService.updateReplicaEntity(event.customerDTO());
    }
}
