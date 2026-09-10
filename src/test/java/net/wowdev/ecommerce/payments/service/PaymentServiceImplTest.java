package net.wowdev.ecommerce.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationService;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationService;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import net.wowdev.ecommerce.domain.events.PaymentCompleted;
import net.wowdev.ecommerce.domain.events.PaymentFailed;
import net.wowdev.ecommerce.payments.TestFixtures;
import net.wowdev.ecommerce.payments.messaging.PaymentProducer;
import net.wowdev.ecommerce.payments.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.MockedStatic;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

class PaymentServiceImplTest {
  private static final UUID ID = TestFixtures.paymentDto().getId();
  private PaymentRepository repository;
  private PaymentServiceImpl service;
  private PaymentProducer paymentProducer;

  private static OrderDTO order() {
    final OrderDTO order = new OrderDTO();
    order.setId(UUID.fromString("22222222-2222-2222-2222-222222222222"));
    order.setCustomerId(UUID.fromString("33333333-3333-3333-3333-333333333333"));
    order.setPaymentMethodId(UUID.fromString("44444444-4444-4444-4444-444444444444"));
    order.setTotalAmount(new BigDecimal("25.00"));
    return order;
  }

  @BeforeEach
  void setUp() {
    repository = mock(PaymentRepository.class);
    PaymentMethodReplicationService paymentMethodRepository = mock(
        PaymentMethodReplicationService.class);
    CustomerReplicationService customerReplicationService = mock(CustomerReplicationService.class);
    paymentProducer = mock(PaymentProducer.class);
    service =
        new PaymentServiceImpl(
            repository, paymentMethodRepository, customerReplicationService, paymentProducer);
    ReflectionTestUtils.setField(service, "failsWhenRunning", false);
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
        .hasMessage("Payment record not found: " + ID);
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
        .hasMessage("Payment record not found: " + ID);
  }

  @Test
  void processesAuthorizedPaymentAndPublishesCompletion() {
    final OrderDTO order = order();
    final PaymentEntity saved = TestFixtures.paymentEntity();
    final Instant oddSecond = Instant.parse("2026-01-01T00:00:01Z");
    when(repository.save(any(PaymentEntity.class))).thenReturn(saved);

    try (MockedStatic<Instant> clock = mockStatic(Instant.class, Answers.CALLS_REAL_METHODS)) {
      clock.when(Instant::now).thenReturn(oddSecond);

      service.process(order);

      verify(repository).save(any(PaymentEntity.class));
      verify(paymentProducer).publish(any(PaymentCompleted.class));
      verify(paymentProducer, never()).publish(any(PaymentFailed.class));
    }
  }

  @Test
  void processesFailedPaymentAndPublishesFailure() {
    final OrderDTO order = order();
    ReflectionTestUtils.setField(service, "failsWhenRunning", true);
    when(repository.save(any(PaymentEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    try (MockedStatic<Instant> clock = mockStatic(Instant.class, Answers.CALLS_REAL_METHODS)) {

      service.process(order);

      verify(repository).save(any(PaymentEntity.class));
      verify(paymentProducer).publish(any(PaymentFailed.class));
      verify(paymentProducer, never()).publish(any(PaymentCompleted.class));
    }
  }
}
