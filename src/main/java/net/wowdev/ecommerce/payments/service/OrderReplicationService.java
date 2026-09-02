package net.wowdev.ecommerce.payments.service;

import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.OrderDTO;

public interface OrderReplicationService {

  OrderDTO findById(UUID id);

  OrderDTO replicate(OrderDTO orderDTO);
}
