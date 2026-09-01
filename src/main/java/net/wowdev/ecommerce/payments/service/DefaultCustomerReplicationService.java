package net.wowdev.ecommerce.payments.service;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.CustomerDTO;
import net.wowdev.ecommerce.domain.entity.CustomerEntity;
import net.wowdev.ecommerce.domain.mapper.CustomerMapper;
import net.wowdev.ecommerce.payments.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultCustomerReplicationService implements CustomerReplicationService {

  private final CustomerRepository repository;

  @Override
  @Transactional(readOnly = true)
  public CustomerDTO findById(final UUID id) {
    return CustomerMapper.toDto(
        repository
            .findById(id)
            .orElseThrow(() -> new CustomerNotFoundException(id.toString())));
  }

  @Override
  @Transactional
  public CustomerDTO replicate(final CustomerDTO customerDTO) {
    final CustomerEntity replicatingEntity = CustomerMapper.toEntity(customerDTO);
    final CustomerEntity entityToSave =
        repository
            .findById(customerDTO.getId())
            .map(
                existingEntity -> {
                  final CustomerEntity updatedEntity =
                      updateCustomerEntity(existingEntity, replicatingEntity);
                  log.debug(">>>> Updating replica for Customer record...");
                  return updatedEntity;
                })
            .orElseGet(
                () -> {
                  log.debug(">>>> Creating replica for Customer record...");
                  return replicatingEntity;
                });
    final CustomerEntity replicatedEntity = repository.save(entityToSave);
    log.debug(">>>> Saved replica for Customer record: {}", replicatedEntity.getId());
    return CustomerMapper.toDto(replicatedEntity);
  }

  protected static CustomerEntity updateCustomerEntity(
      CustomerEntity existingEntity, CustomerEntity toReplicateEntity) {
    existingEntity.setFirstName(toReplicateEntity.getFirstName());
    existingEntity.setLastName(toReplicateEntity.getLastName());
    existingEntity.setEmail(toReplicateEntity.getEmail());
    existingEntity.setCustomerStatus(toReplicateEntity.getCustomerStatus());
    existingEntity.setAddressLine1(toReplicateEntity.getAddressLine1());
    existingEntity.setAddressLine2(toReplicateEntity.getAddressLine2());
    existingEntity.setCity(toReplicateEntity.getCity());
    existingEntity.setStateProvince(toReplicateEntity.getStateProvince());
    existingEntity.setPostalCode(toReplicateEntity.getPostalCode());
    existingEntity.setCountry(toReplicateEntity.getCountry());
    return existingEntity;
  }
}
