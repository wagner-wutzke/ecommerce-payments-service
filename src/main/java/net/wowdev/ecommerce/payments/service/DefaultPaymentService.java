package net.wowdev.ecommerce.payments.service;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.CustomerDTO;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.dto.PaymentMethodDTO;
import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import net.wowdev.ecommerce.domain.enums.PaymentMethod;
import net.wowdev.ecommerce.domain.enums.PaymentStatus;
import net.wowdev.ecommerce.domain.events.PaymentCompletedEvent;
import net.wowdev.ecommerce.domain.mapper.PaymentMapper;
import net.wowdev.ecommerce.domain.mapper.PaymentMethodMapper;
import net.wowdev.ecommerce.payments.messaging.PaymentProducer;
import net.wowdev.ecommerce.payments.repository.PaymentMethodRepository;
import net.wowdev.ecommerce.payments.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPaymentService implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final PaymentMethodReplicationService paymentMethodService;
  private final CustomerReplicationService customerService;
  private final PaymentProducer paymentProducer;

  @Override
  @Transactional(readOnly = true)
  public PaymentDTO findById(final UUID id) {
    return paymentRepository
        .findById(id)
        .map(PaymentMapper::toDto)
        .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
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
            .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
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
      throw new PaymentNotFoundException("Payment not found: " + id);
    }
    paymentRepository.deleteById(id);
  }

  @Override
  @Transactional
  public void process(OrderDTO orderDTO) {
    log.debug(">>>> Processing Payment for OrderDTO started: {}", orderDTO.getId());
    PaymentMethodDTO paymentMethodDTO = paymentMethodService.findById(orderDTO.getPaymentMethodId());
    CustomerDTO customerDTO = customerService.findById(orderDTO.getCustomerId());
    PaymentDTO paymentDTO = new PaymentDTO(
        null,
        orderDTO.getId(),
        customerDTO.getId(),
        paymentMethodDTO.getId(),
        "payment-token",
        PaymentStatus.PENDING,
        orderDTO.getTotalAmount(),
        PaymentMethod.CREDIT_CARD,
        null,
        null
    );
    PaymentDTO createdDTO = this.create(paymentDTO);
    log.debug(">>>> Processing Payment for OrderDTO finished successfully: {}", orderDTO.getId());
    paymentProducer.publish(
        new PaymentCompletedEvent(
            UUID.randomUUID(),
            orderDTO.getId().toString(),
            createdDTO,
            Instant.now(),
            PaymentProducer.ORIGIN_SERVICE
        )
    );
  }
}
