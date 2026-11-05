package net.wowdev.ecommerce.payments.messaging;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class CircuitBreakerListenerController {

  private final CircuitBreakerRegistry circuitBreakerRegistry;
  private final KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  // Matches the class-level @KafkaListener id property
  @Value("${app.kafka.payments-event-listener-id}")
  private String eventListenerId;

  // the same name as defined in the application.yaml
  private static final String CIRCUIT_BREAKER_NAME = "ecommerceCircuitBreakerGateway";

  public CircuitBreakerListenerController(CircuitBreakerRegistry circuitBreakerRegistry,
      KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry) {
    this.circuitBreakerRegistry = circuitBreakerRegistry;
    this.kafkaListenerEndpointRegistry = kafkaListenerEndpointRegistry;
    log.info(">> CircuitBreakerListenerController created.");
  }

  @PostConstruct
  public void initLifecycleWatchers() {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    log.debug(">> Circuit Breaker event publisher initialized.");

    circuitBreaker.getEventPublisher().onStateTransition(event -> {
      CircuitBreaker.State targetState = event.getStateTransition().getToState();
      log.debug(">> Circuit Breaker '{}' shifted to: {}", CIRCUIT_BREAKER_NAME, targetState);

      switch (targetState) {
        case OPEN -> {
          pauseWholeContainer();
        }
        case CLOSED, HALF_OPEN -> {
          resumeWholeContainer();
        }
        default -> {
        }
      }
    });
  }

  private void pauseWholeContainer() {
    MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(
        eventListenerId);
    if (container != null && container.isRunning()) {
      log.warn(">> Pausing handler listener stream context: {}", eventListenerId);
      container.pause();
    }
  }

  private void resumeWholeContainer() {
    MessageListenerContainer container = kafkaListenerEndpointRegistry.getListenerContainer(
        eventListenerId);
    log.debug(">> Re-activating handler container loop: {}", eventListenerId);
    if (container != null) {
      container.resume();
    }
  }
}
