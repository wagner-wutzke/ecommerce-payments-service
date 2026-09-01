package net.wowdev.ecommerce.payments.service;

import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PaymentService {
  PaymentDTO findById(UUID id);

  Page<PaymentDTO> findAll(Pageable pageable);

  PaymentDTO create(PaymentDTO payment);

  PaymentDTO update(UUID id, PaymentDTO payment);

  void delete(UUID id);

  void process(OrderDTO orderDTO);
}