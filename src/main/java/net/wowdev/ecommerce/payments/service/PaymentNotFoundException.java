package net.wowdev.ecommerce.payments.service;

public class PaymentNotFoundException extends RuntimeException {
  public PaymentNotFoundException(final String message) {
    super(message);
  }
}