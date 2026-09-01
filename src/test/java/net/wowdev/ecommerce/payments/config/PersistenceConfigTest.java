package net.wowdev.ecommerce.payments.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PersistenceConfigTest {
  @Test
  void createsPersistenceConfiguration() {
    assertThat(new PersistenceConfig()).isNotNull();
  }
}
