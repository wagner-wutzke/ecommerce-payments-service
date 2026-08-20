package net.wowdev.ecommerce.payments.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PaymentProducer {
    private final KafkaTemplate<String, PaymentChangeEvent> template;
    private final String topic;

    public PaymentProducer(final KafkaTemplate<String, PaymentChangeEvent> template,
                           @Value("${app.kafka.payment-changes-topic}") final String topic) {
        this.template = template;
        this.topic = topic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void publishAfterCommit(final PaymentChangeEvent event) {
        template.send(topic, event.payload().getId().toString(), event);
    }
}
