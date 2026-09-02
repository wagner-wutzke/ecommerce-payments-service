package net.wowdev.ecommerce.payments;

import net.wowdev.ecommerce.datareplication.config.ReplicationPersistenceConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication(
    scanBasePackages = {"net.wowdev.ecommerce.payments", "net.wowdev.ecommerce.datareplication"})
@EntityScan(basePackages = "net.wowdev.ecommerce.domain.entity")
@Import(ReplicationPersistenceConfig.class)
public class PaymentsServiceApplication {
  public static void main(final String[] args) {
    SpringApplication.run(PaymentsServiceApplication.class, args);
  }
}
