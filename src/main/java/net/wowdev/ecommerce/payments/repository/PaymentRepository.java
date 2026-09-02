package net.wowdev.ecommerce.payments.repository;

import java.util.UUID;
import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, UUID> {}
