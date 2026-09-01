package net.wowdev.ecommerce.payments.service;

public class CustomerNotFoundException extends RuntimeException {
  public CustomerNotFoundException(final String id) {
    super("Customer not found: " + id);
  }
}
