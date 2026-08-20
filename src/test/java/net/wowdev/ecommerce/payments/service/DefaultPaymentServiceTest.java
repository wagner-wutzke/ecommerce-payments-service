package net.wowdev.ecommerce.payments.service;

import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import net.wowdev.ecommerce.payments.TestFixtures;
import net.wowdev.ecommerce.payments.messaging.PaymentChangeEvent;
import net.wowdev.ecommerce.payments.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultPaymentServiceTest {
    @Mock
    private PaymentRepository repository;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    private DefaultPaymentService service;

    @BeforeEach
    void setUp() {
        service = new DefaultPaymentService(repository, eventPublisher);
    }

    @Test
    void readsAndCreatesPayments() {
        final PaymentEntity entity = TestFixtures.paymentEntity();
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(repository.findAll(any(PageRequest.class))).thenReturn(new PageImpl<>(List.of(entity)));
        when(repository.save(any(PaymentEntity.class))).thenReturn(entity);

        assertThat(service.findById(entity.getId()).getTransactionId()).isEqualTo("TX-1");
        assertThat(service.findAll(PageRequest.of(0, 10))).hasSize(1);
        assertThat(service.create(TestFixtures.paymentDto()).getId()).isEqualTo(entity.getId());
        verify(eventPublisher).publishEvent(any(PaymentChangeEvent.class));
    }

    @Test
    void updatesAndDeletesPayments() {
        final PaymentEntity entity = TestFixtures.paymentEntity();
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(repository.save(any(PaymentEntity.class))).thenReturn(entity);
        assertThat(service.update(entity.getId(), TestFixtures.paymentDto())).isNotNull();
        verify(eventPublisher).publishEvent(any(PaymentChangeEvent.class));

        when(repository.existsById(entity.getId())).thenReturn(true);
        service.delete(entity.getId());
        verify(repository).deleteById(entity.getId());
    }

    @Test
    void rejectsMissingPayments() {
        final UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findById(id)).isInstanceOf(PaymentNotFoundException.class);
        assertThatThrownBy(() -> service.update(id, TestFixtures.paymentDto()))
                .isInstanceOf(PaymentNotFoundException.class);
        when(repository.existsById(id)).thenReturn(false);
        assertThatThrownBy(() -> service.delete(id)).isInstanceOf(PaymentNotFoundException.class);
    }
}
