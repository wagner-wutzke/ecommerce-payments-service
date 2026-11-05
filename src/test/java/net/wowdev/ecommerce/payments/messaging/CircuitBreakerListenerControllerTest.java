package net.wowdev.ecommerce.payments.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.EventConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.KafkaListenerEndpointRegistry;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.test.util.ReflectionTestUtils;

class CircuitBreakerListenerControllerTest {
    private static final String LISTENER_ID = "payments-listener";

    private final CircuitBreakerRegistry circuitBreakerRegistry = mock(CircuitBreakerRegistry.class);
    private final KafkaListenerEndpointRegistry listenerRegistry = mock(KafkaListenerEndpointRegistry.class);
    private final CircuitBreaker circuitBreaker = mock(CircuitBreaker.class);
    private final CircuitBreaker.EventPublisher eventPublisher = mock(CircuitBreaker.EventPublisher.class);
    private final MessageListenerContainer container = mock(MessageListenerContainer.class);
    private EventConsumer<CircuitBreakerOnStateTransitionEvent> transitionListener;

    @BeforeEach
    void setUp() {
        when(circuitBreakerRegistry.circuitBreaker("ecommerceCircuitBreakerGateway")).thenReturn(circuitBreaker);
        when(listenerRegistry.getListenerContainer(LISTENER_ID)).thenReturn(container);
        when(circuitBreaker.getEventPublisher()).thenReturn(eventPublisher);
        when(eventPublisher.onStateTransition(any())).thenAnswer(invocation -> {
            transitionListener = invocation.getArgument(0);
            return eventPublisher;
        });
    }

    @Test
    void openStatePausesRunningContainer() {
        final CircuitBreakerListenerController controller = controller();
        when(container.isRunning()).thenReturn(true);

        transition(CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);

        verify(container).pause();
        verify(container, never()).resume();
    }

    @Test
    void openStateDoesNotPauseStoppedContainer() {
        final CircuitBreakerListenerController controller = controller();
        when(container.isRunning()).thenReturn(false);

        transition(CircuitBreaker.State.CLOSED, CircuitBreaker.State.OPEN);

        verify(container, never()).pause();
    }

    @Test
    void closedAndHalfOpenStatesResumePausedContainer() {
        final CircuitBreakerListenerController controller = controller();
        doReturn(true).when(container).isContainerPaused();

        transition(CircuitBreaker.State.OPEN, CircuitBreaker.State.CLOSED);
        transition(CircuitBreaker.State.OPEN, CircuitBreaker.State.HALF_OPEN);

        verify(container, org.mockito.Mockito.times(2)).resume();
        verify(container, never()).pause();
    }

    @Test
    void otherStatesDoNotChangeContainer() {
        final CircuitBreakerListenerController controller = controller();

        transition(CircuitBreaker.State.CLOSED, CircuitBreaker.State.DISABLED);

        verify(listenerRegistry, never()).getListenerContainer(any());
    }

    private CircuitBreakerListenerController controller() {
        final CircuitBreakerListenerController controller =
                new CircuitBreakerListenerController(circuitBreakerRegistry, listenerRegistry);
        ReflectionTestUtils.setField(controller, "eventListenerId", LISTENER_ID);
        controller.initLifecycleWatchers();
        return controller;
    }

    private void transition(CircuitBreaker.State from, CircuitBreaker.State target) {
        final CircuitBreakerOnStateTransitionEvent event = mock(CircuitBreakerOnStateTransitionEvent.class);
        when(event.getStateTransition()).thenReturn(CircuitBreaker.StateTransition.transitionBetween("test", from, target));
        transitionListener.consumeEvent(event);
    }
}
