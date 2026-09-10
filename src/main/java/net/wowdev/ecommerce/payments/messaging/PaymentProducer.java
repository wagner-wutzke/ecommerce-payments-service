package net.wowdev.ecommerce.payments.messaging;

import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.events.PaymentCompleted;
import net.wowdev.ecommerce.domain.events.PaymentFailed;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
public class PaymentProducer {
  private final KafkaTemplate<String, Object> template;
  private final String topic;

  public PaymentProducer(
      final KafkaTemplate<String, Object> template,
      @Value("${app.kafka.payments-topic}") final String topic) {
    this.template = template;
    this.topic = topic;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(PaymentCompleted event) {
    log.debug(">> Publishing PaymentCompleted event: {}", event.eventId());
    template.send(topic, event.paymentDTO().getId().toString(), event);
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void publish(PaymentFailed event) {
    log.debug(">> Publishing PaymentFailed event: {}", event.eventId());
    template.send(topic, event.orderDTO().getId().toString(), event);
  }
}
