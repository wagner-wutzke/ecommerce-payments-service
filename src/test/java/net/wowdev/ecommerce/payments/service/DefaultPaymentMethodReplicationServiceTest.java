package net.wowdev.ecommerce.payments.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import net.wowdev.ecommerce.domain.dto.PaymentMethodDTO;
import net.wowdev.ecommerce.domain.entity.PaymentMethodEntity;
import net.wowdev.ecommerce.payments.repository.PaymentMethodRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DefaultPaymentMethodReplicationServiceTest {
  private static final UUID ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
  private PaymentMethodRepository repository;
  private DefaultPaymentMethodReplicationService service;

  private static PaymentMethodDTO dto(String owner) {
    return new PaymentMethodDTO(ID, ID, "4111", owner, "12/30", 123, "Visa", Instant.now(), Instant.now());
  }

  private static PaymentMethodEntity entity(String owner) {
    final PaymentMethodDTO value = dto(owner);
    return new PaymentMethodEntity(value.getId(), value.getCustomerId(), value.getCardNumber(), value.getOwnerName(), value.getExpiration(), value.getCvv(), value.getCardName(), value.getCreatedAt(), value.getModifiedAt());
  }

  @BeforeEach
  void setUp() {
    repository = mock(PaymentMethodRepository.class);
    service = new DefaultPaymentMethodReplicationService(repository);
  }

  @Test
  void findsAndReplicatesNewMethod() {
    when(repository.findById(ID)).thenReturn(Optional.empty());
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service.replicate(dto("Owner")).getOwnerName()).isEqualTo("Owner");
  }

  @Test
  void updatesExistingMethodAndHandlesMissingRead() {
    final PaymentMethodEntity existing = entity("Old");
    when(repository.findById(ID)).thenReturn(Optional.of(existing));
    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    assertThat(service.replicate(dto("New")).getOwnerName()).isEqualTo("New");
    when(repository.findById(ID)).thenReturn(Optional.empty());
    assertThatThrownBy(() -> service.findById(ID))
        .isInstanceOf(PaymentMethodNotFoundException.class)
        .hasMessage("PaymentMethod not found: " + ID);
  }

  @Test
  void findsExistingMethod() {
    when(repository.findById(ID)).thenReturn(Optional.of(entity("Owner")));
    assertThat(service.findById(ID).getOwnerName()).isEqualTo("Owner");
  }
}
