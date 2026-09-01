package net.wowdev.ecommerce.payments.service;

import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.entity.OrderEntity;
import net.wowdev.ecommerce.domain.entity.OrderLineEntity;
import net.wowdev.ecommerce.domain.mapper.OrderMapper;
import net.wowdev.ecommerce.payments.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DefaultOrderReplicationService implements OrderReplicationService {

  private final OrderRepository orderRepository;

  @Override
  @Transactional(readOnly = true)
  public OrderDTO findById(final UUID id) {
    return OrderMapper.toDto(
        orderRepository.findById(id).orElseThrow(() -> new OrderNotFoundException(id.toString())));
  }

  @Override
  @Transactional
  public OrderDTO replicate(final OrderDTO orderDTO) {
    OrderEntity replicaEntity;
    OrderEntity toReplicateEntity = OrderMapper.toEntity(orderDTO);
    Optional<OrderEntity> existingEntityOpt = orderRepository.findById(orderDTO.getId());

    if (existingEntityOpt.isEmpty()) {
      log.debug(">>>> Creating replica for Order record: {}", toReplicateEntity.getId());
      replicaEntity = orderRepository.save(toReplicateEntity);
    } else {
      final OrderEntity entity = updateOrderEntity(existingEntityOpt.get(), toReplicateEntity);
      log.debug(">>>> Updating replica for Order record: {}", entity.getId());
      replicaEntity = orderRepository.save(entity);
    }
    log.debug(">>>> Saved replica for Order record: {}", replicaEntity.getId());
    return OrderMapper.toDto(replicaEntity);
  }

  protected static OrderEntity updateOrderEntity(
      OrderEntity existingEntity, OrderEntity toReplicateEntity) {
    existingEntity.setCustomerId(toReplicateEntity.getCustomerId());
    existingEntity.setOrderAmount(toReplicateEntity.getOrderAmount());
    existingEntity.setDiscountAmount(toReplicateEntity.getDiscountAmount());
    existingEntity.setTaxAmount(toReplicateEntity.getTaxAmount());
    existingEntity.setTotalAmount(toReplicateEntity.getTotalAmount());
    existingEntity.setShippingAmount(toReplicateEntity.getShippingAmount());
    existingEntity.setOrderNumber(toReplicateEntity.getOrderNumber());
    existingEntity.setPaymentMethodId(toReplicateEntity.getPaymentMethodId());
    existingEntity.setOrderStatus(toReplicateEntity.getOrderStatus());

    for (OrderLineEntity orderLine : existingEntity.getOrderLines()) {
      OrderLineEntity toUpdate =
          existingEntity.getOrderLines().stream()
              .filter(ol -> ol.getId().equals(orderLine.getId()))
              .findFirst()
              .orElse(null);
      if (toUpdate != null) {
        toUpdate.setPrice(orderLine.getPrice());
        toUpdate.setQuantity(orderLine.getQuantity());
        toUpdate.setProductId(orderLine.getProductId());
      }
    }
    return existingEntity;
  }
}
