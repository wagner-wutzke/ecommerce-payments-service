package net.wowdev.ecommerce.payments.service;

import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.PaymentMethodDTO;

public interface PaymentMethodReplicationService {
  PaymentMethodDTO findById(UUID id);

  PaymentMethodDTO replicate(PaymentMethodDTO paymentMethodDTO);
}
