package net.wowdev.ecommerce.payments.service;

import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.datareplication.service.CustomerReplicationService;
import net.wowdev.ecommerce.datareplication.service.PaymentMethodReplicationService;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import net.wowdev.ecommerce.domain.enums.PaymentMethod;
import net.wowdev.ecommerce.domain.enums.PaymentStatus;
import net.wowdev.ecommerce.domain.events.PaymentCompleted;
import net.wowdev.ecommerce.domain.events.PaymentFailed;
import net.wowdev.ecommerce.domain.mapper.PaymentMapper;
import net.wowdev.ecommerce.payments.messaging.PaymentProducer;
import net.wowdev.ecommerce.payments.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMethodReplicationService paymentMethodService;
  private final CustomerReplicationService customerService;
  private final PaymentProducer paymentProducer;

  @Value(value = "${app.service.payments.failing}")
  private boolean failsWhenRunning;

  @Override
  @Transactional(readOnly = true)
  public PaymentDTO findById(final UUID id) {
    return paymentRepository
        .findById(id)
        .map(PaymentMapper::toDto)
        .orElseThrow(() -> new PaymentNotFoundException("Payment record not found: " + id));
  }

  @Override
  @Transactional(readOnly = true)
  public Page<PaymentDTO> findAll(final Pageable pageable) {
    return paymentRepository.findAll(pageable).map(PaymentMapper::toDto);
  }

  @Override
  @Transactional
  public PaymentDTO create(final PaymentDTO payment) {
    final PaymentEntity saved = paymentRepository.save(PaymentMapper.toEntity(payment));
    return PaymentMapper.toDto(saved);
  }

  @Override
  @Transactional
  public PaymentDTO update(final UUID id, final PaymentDTO payment) {
    final PaymentEntity current =
        paymentRepository
            .findById(id)
            .orElseThrow(() -> new PaymentNotFoundException("Payment record not found: " + id));
    final PaymentDTO replacement = PaymentMapper.toDto(current);
    replacement.setAmount(payment.getAmount());
    replacement.setPaymentMethod(payment.getPaymentMethod());
    replacement.setPaymentStatus(payment.getPaymentStatus());
    return PaymentMapper.toDto(paymentRepository.save(PaymentMapper.toEntity(replacement)));
  }

  @Override
  @Transactional
  public void delete(final UUID id) {
    if (!paymentRepository.existsById(id)) {
      throw new PaymentNotFoundException("Payment record not found: " + id);
    }
    paymentRepository.deleteById(id);
  }

  @Override
  @Transactional
  public void process(OrderDTO orderDTO) {
    log.debug(">> Processing Payment for order: {}", orderDTO.getId());

    // get replicated data for processing the payment
    // PaymentMethodDTO paymentMethodDTO =
    //   paymentMethodService.findById(orderDTO.getPaymentMethodId());
    // CustomerDTO customerDTO = customerService.findById(orderDTO.getCustomerId());

    PaymentDTO paymentDTO =
        new PaymentDTO(
            null,
            orderDTO.getId(),
            orderDTO.getCustomerId(),
            orderDTO.getPaymentMethodId(),
            "payment-token",
            "tx-" + orderDTO.getId(),
            PaymentStatus.PENDING,
            orderDTO.getTotalAmount(),
            PaymentMethod.CREDIT_CARD,
            null,
            null);

    try {

      if (failsWhenRunning()) {
        throw new RuntimeException(
            "Payment could not be processed. Payment status: REJECTED.");
      }
      log.debug(">> Payment for order {} successfully finished.", orderDTO.getId());

      paymentDTO.setPaymentStatus(PaymentStatus.AUTHORIZED);
      PaymentDTO createdDTO = this.create(paymentDTO);
      publishPaymentCompletedEvent(orderDTO, createdDTO);

    } catch (RuntimeException e) {
      log.debug(">> Payment for order {} failed. Reason: {}", orderDTO.getId(), e.getMessage());
      paymentDTO.setPaymentStatus(PaymentStatus.REJECTED);
      paymentRepository.save(PaymentMapper.toEntity(paymentDTO));
      publishPaymentFailedEvent(orderDTO, e.getMessage());
    }
  }

  @Transactional
  @Override
  public void compensate(OrderDTO orderDTO, String reason) {
    log.debug(">> Compensating Payment for order: {}", orderDTO.getId());
    publishPaymentFailedEvent(orderDTO, reason);
  }

  private void publishPaymentFailedEvent(OrderDTO orderDTO, String reason) {
    paymentProducer.publish(
        new PaymentFailed(
            UUID.randomUUID(),
            orderDTO.getId().toString(),
            orderDTO,
            reason,
            Instant.now(),
            PaymentService.ORIGIN_SERVICE));
  }

  private void publishPaymentCompletedEvent(OrderDTO orderDTO, PaymentDTO createdDTO) {
    paymentProducer.publish(
        new PaymentCompleted(
            UUID.randomUUID(),
            orderDTO.getId().toString(),
            orderDTO,
            createdDTO,
            Instant.now(),
            PaymentService.ORIGIN_SERVICE));
  }

  private boolean failsWhenRunning() {
    if (this.failsWhenRunning) {
      log.debug(
          """
          >> Service is configured to be failing when processing events. "
             This option can be configured with the "SERVICE_PAYMENTS_FAILING" environment variable.
          """);
    }
    return this.failsWhenRunning;
  }
}
