package net.wowdev.ecommerce.payments.service;

import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.CustomerDTO;

public interface CustomerReplicationService {

  CustomerDTO findById(UUID id);

  CustomerDTO replicate(CustomerDTO customer);
}
