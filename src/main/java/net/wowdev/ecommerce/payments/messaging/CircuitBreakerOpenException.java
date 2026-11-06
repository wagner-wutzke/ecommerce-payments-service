package net.wowdev.ecommerce.payments.messaging;

/**
 * Indicates that payment processing was rejected because its circuit breaker is open.
 * This is distinct from a failure raised while actually processing a payment.
 */
public class CircuitBreakerOpenException extends RuntimeException {

  public CircuitBreakerOpenException(String message, Throwable cause) {
    super(message, cause);
  }
}
