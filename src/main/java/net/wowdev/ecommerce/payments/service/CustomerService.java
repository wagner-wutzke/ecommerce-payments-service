package net.wowdev.ecommerce.payments.service;

import net.wowdev.ecommerce.domain.dto.CustomerDTO;

import java.util.UUID;

public interface CustomerService {

    CustomerDTO findById(UUID id);

    CustomerDTO updateReplicaEntity(CustomerDTO customer);

}
