package net.wowdev.ecommerce.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import net.wowdev.ecommerce.payments.TestFixtures;
import net.wowdev.ecommerce.payments.messaging.PaymentProducer;
import net.wowdev.ecommerce.payments.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

class DefaultPaymentServiceTest {
  private static final UUID ID = TestFixtures.paymentDto().getId();
  private PaymentRepository repository;
  private DefaultPaymentService service;
  private PaymentMethodReplicationService paymentMethodRepository;
  private CustomerReplicationService customerReplicationService;
  private PaymentProducer paymentProducer;

  @BeforeEach
  void setUp() {
    repository = mock(PaymentRepository.class);
    service =
        new DefaultPaymentService(
            repository, paymentMethodRepository, customerReplicationService, paymentProducer);
  }

  @Test
  void findsAndListsPayments() {
    final PaymentEntity entity = TestFixtures.paymentEntity();
    when(repository.findById(ID)).thenReturn(Optional.of(entity));
    when(repository.findAll(any(PageRequest.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(entity)));

    assertThat(service.findById(ID).getId()).isEqualTo(ID);
    assertThat(service.findAll(PageRequest.of(0, 10))).hasSize(1);
  }

  @Test
  void throwsWhenPaymentIsMissing() {
    when(repository.findById(ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.findById(ID))
        .isInstanceOf(PaymentNotFoundException.class)
        .hasMessage("Payment not found: " + ID);
    assertThatThrownBy(() -> service.update(ID, TestFixtures.paymentDto()))
        .isInstanceOf(PaymentNotFoundException.class);
  }

  @Test
  void createsAndUpdatesPayment() {
    final PaymentEntity entity = TestFixtures.paymentEntity();
    when(repository.save(any(PaymentEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(repository.findById(ID)).thenReturn(Optional.of(entity));

    final PaymentDTO payment = TestFixtures.paymentDto();
    assertThat(service.create(payment).getId()).isEqualTo(ID);
    assertThat(service.update(ID, payment).getId()).isEqualTo(ID);
    verify(repository, times(2)).save(any(PaymentEntity.class));
  }

  @Test
  void deletesExistingPaymentAndRejectsMissingPayment() {
    when(repository.existsById(ID)).thenReturn(true, false);
    service.delete(ID);
    verify(repository).deleteById(ID);
    assertThatThrownBy(() -> service.delete(ID))
        .isInstanceOf(PaymentNotFoundException.class)
        .hasMessage("Payment not found: " + ID);
  }
}
