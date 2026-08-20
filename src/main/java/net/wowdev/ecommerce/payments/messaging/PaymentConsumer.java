package net.wowdev.ecommerce.payments.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PaymentConsumer {
    @KafkaListener(topics = "${app.kafka.payment-changes-topic}", groupId = "${app.kafka.consumer-group}")
    public void consume(final PaymentChangeEvent event) {
        log.info("Consumed payment change event type={} id={}", event.eventType(), event.payload().getId());
    }
}
