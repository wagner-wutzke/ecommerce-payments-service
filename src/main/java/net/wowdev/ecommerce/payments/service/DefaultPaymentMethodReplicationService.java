package net.wowdev.ecommerce.payments.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.PaymentMethodDTO;
import net.wowdev.ecommerce.domain.entity.PaymentMethodEntity;
import net.wowdev.ecommerce.domain.mapper.PaymentMethodMapper;
import net.wowdev.ecommerce.payments.repository.PaymentMethodRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultPaymentMethodReplicationService implements PaymentMethodReplicationService {

  private final PaymentMethodRepository repository;

  @Override
  @Transactional(readOnly = true)
  public PaymentMethodDTO findById(final UUID id) {
    return PaymentMethodMapper.toDto(
        repository
            .findById(id)
            .orElseThrow(
                () -> new PaymentMethodNotFoundException("PaymentMethod not found: " + id)));
  }

  @Override
  @Transactional
  public PaymentMethodDTO replicate(PaymentMethodDTO paymentMethodDTO) {
    final PaymentMethodEntity replicatingEntity = PaymentMethodMapper.toEntity(paymentMethodDTO);
    final PaymentMethodEntity entityToSave =
        repository
            .findById(paymentMethodDTO.getId())
            .map(
                existingEntity -> {
                  final PaymentMethodEntity updatedEntity =
                      updatePaymentMethodEntity(existingEntity, replicatingEntity);
                  log.debug(">>>> Updating replica for PaymentMethod record...");
                  return updatedEntity;
                })
            .orElseGet(
                () -> {
                  log.debug(">>>> Creating replica for PaymentMethod record...");
                  return replicatingEntity;
                });
    final PaymentMethodEntity replicatedEntity = repository.save(entityToSave);
    log.debug(">>>> Saved replica for PaymentMethod record: {}", replicatedEntity.getId());
    return PaymentMethodMapper.toDto(replicatedEntity);
  }

  protected static PaymentMethodEntity updatePaymentMethodEntity(
      PaymentMethodEntity existingEntity, PaymentMethodEntity toReplicateEntity) {
    existingEntity.setOwnerName(toReplicateEntity.getOwnerName());
    existingEntity.setCardName(toReplicateEntity.getCardName());
    existingEntity.setExpiration(toReplicateEntity.getExpiration());
    existingEntity.setCardNumber(toReplicateEntity.getCardNumber());
    existingEntity.setCvv(toReplicateEntity.getCvv());
    existingEntity.setCustomerId(toReplicateEntity.getCustomerId());
    return existingEntity;
  }
}
