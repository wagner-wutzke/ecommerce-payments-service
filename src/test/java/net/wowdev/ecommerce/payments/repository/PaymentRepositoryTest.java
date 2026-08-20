package net.wowdev.ecommerce.payments.repository;

import net.wowdev.ecommerce.payments.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PaymentRepositoryTest {
    @Autowired
    private PaymentRepository repository;

    @Test
    void persistsAndReadsPayment() {
        repository.save(TestFixtures.paymentEntity());
        assertThat(repository.findById(TestFixtures.paymentDto().getId())).isPresent();
    }
}
