package net.wowdev.ecommerce.payments.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {
    @Autowired
    private PaymentRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void readsPaymentInsertedInDatabase() {
        final UUID paymentId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        final UUID orderId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        final UUID customerId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        final UUID paymentMethodId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        final Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");

        jdbcTemplate.update("""
                                    INSERT INTO payments (
                                        id, order_id, customer_id, payment_method_id, transaction_id,
                                        payment_token, amount, payment_method, payment_status, created_at
                                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                                    """,
                            paymentId, orderId, customerId, paymentMethodId, "TX-1", "TOKEN-1",
                            10.00, "PIX", "PENDING", createdAt);

        assertThat(repository.findById(paymentId))
                .get()
                .satisfies(payment -> {
                    assertThat(payment.getId()).isEqualTo(paymentId);
                    assertThat(payment.getOrderId()).isEqualTo(orderId);
                    assertThat(payment.getCustomerId()).isEqualTo(customerId);
                    assertThat(payment.getPaymentMethodId()).isEqualTo(paymentMethodId);
                    assertThat(payment.getTransactionId()).isEqualTo("TX-1");
                });
    }
}
