package net.wowdev.ecommerce.payments.service;

public class OrderNotFoundException extends RuntimeException {
  public OrderNotFoundException(final String message) {
    super(message);
  }
}
