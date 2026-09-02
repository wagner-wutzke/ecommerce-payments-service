package net.wowdev.ecommerce.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.OrderDTO;
import net.wowdev.ecommerce.domain.dto.OrderLineDTO;
import net.wowdev.ecommerce.domain.entity.OrderEntity;
import net.wowdev.ecommerce.domain.entity.OrderLineEntity;
import net.wowdev.ecommerce.domain.enums.OrderStatus;
import net.wowdev.ecommerce.payments.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultOrderReplicationServiceTest {
  private static final UUID ID = UUID.fromString("33333333-3333-3333-3333-333333333333");
  private OrderRepository repository;
  private DefaultOrderReplicationService service;

  private static OrderDTO dto() {
    final OrderLineDTO line = new OrderLineDTO(ID, ID, ID, 2, BigDecimal.TEN, Instant.now(), Instant.now());
    return new OrderDTO(ID, ID, ID, OrderStatus.PENDING, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.ONE,
        BigDecimal.ONE, BigDecimal.TEN, "ORD-1", List.of(line), Instant.now(), Instant.now());
  }

  private static OrderEntity entity() {
    final OrderDTO value = dto();
    final OrderLineEntity line = new OrderLineEntity(ID, ID, ID, 2, BigDecimal.TEN, Instant.now(), Instant.now());
    return new OrderEntity(value.getId(), value.getCustomerId(), value.getPaymentMethodId(), value.getOrderStatus(),
        value.getTotalAmount(), value.getShippingAmount(), value.getTaxAmount(), value.getDiscountAmount(),
        value.getOrderAmount(), value.getOrderNumber(), List.of(line), value.getCreatedAt(), value.getModifiedAt());
  }

  @BeforeEach
  void setUp() {
    repository = mock(OrderRepository.class);
    service = new DefaultOrderReplicationService(repository);
  }

  @Test
  void findsMissingAndReplicatesNewOrder() {
    when(repository.findById(ID)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service.replicate(dto()).getId()).isEqualTo(ID);
    assertThatThrownBy(() -> service.findById(ID)).isInstanceOf(OrderNotFoundException.class);
  }

  @Test
  void findsAndUpdatesExistingOrder() {
    final OrderEntity existing = entity();
    when(repository.findById(ID)).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service.findById(ID).getId()).isEqualTo(ID);
    assertThat(service.replicate(dto()).getOrderNumber()).isEqualTo("ORD-1");
    verify(repository).save(existing);
  }
}
