package net.wowdev.ecommerce.payments.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.CustomerDTO;
import net.wowdev.ecommerce.domain.entity.CustomerEntity;
import net.wowdev.ecommerce.domain.mapper.CustomerMapper;
import net.wowdev.ecommerce.payments.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@AllArgsConstructor
public class DefaultCustomerService implements CustomerService {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public CustomerDTO findById(final UUID id) {
        return CustomerMapper.toDto(customerRepository.findById(id).orElseThrow(() -> new CustomerNotFoundException(id)));
    }

    @Override
    @Transactional
    public CustomerDTO updateReplicaEntity(final CustomerDTO customer) {
        log.debug(">>>> Saving CustomerDTO: {}", customer);
        CustomerEntity replicaEntity;
        CustomerEntity mappedEntity = CustomerMapper.toEntity(customer);
        Optional<CustomerEntity> currentEntityOptional = customerRepository.findById(customer.getId());
        if (currentEntityOptional.isEmpty()) {
            replicaEntity = customerRepository.save(mappedEntity);
        } else {
            final CustomerEntity entity = getCustomerEntity(currentEntityOptional.get(), mappedEntity);
            replicaEntity = customerRepository.save(entity);
        }
        log.debug(">>>> Saved CustomerEntity: {}", replicaEntity);
        return CustomerMapper.toDto(replicaEntity);
    }

    private static CustomerEntity getCustomerEntity(CustomerEntity entity, CustomerEntity mappedEntity) {
        entity.setFirstName(mappedEntity.getFirstName());
        entity.setLastName(mappedEntity.getLastName());
        entity.setEmail(mappedEntity.getEmail());
        entity.setCustomerStatus(mappedEntity.getCustomerStatus());
        entity.setAddressLine1(mappedEntity.getAddressLine1());
        entity.setAddressLine2(mappedEntity.getAddressLine2());
        entity.setCity(mappedEntity.getCity());
        entity.setStateProvince(mappedEntity.getStateProvince());
        entity.setPostalCode(mappedEntity.getPostalCode());
        entity.setCountry(mappedEntity.getCountry());
        entity.setCreatedAt(mappedEntity.getCreatedAt());
        entity.setModifiedAt(mappedEntity.getModifiedAt());
        return entity;
    }
}
