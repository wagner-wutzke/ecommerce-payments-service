package net.wowdev.ecommerce.payments.service;

public class PaymentMethodNotFoundException extends RuntimeException {
  public PaymentMethodNotFoundException(final String message) {
    super(message);
  }
}
