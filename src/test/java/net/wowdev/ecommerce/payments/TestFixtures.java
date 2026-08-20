package net.wowdev.ecommerce.payments;

import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import net.wowdev.ecommerce.domain.entity.PaymentMethod;
import net.wowdev.ecommerce.domain.entity.PaymentStatus;
import net.wowdev.ecommerce.domain.mapper.PaymentMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class TestFixtures {
    private TestFixtures() {
    }

    public static PaymentDTO paymentDto() {
        final PaymentDTO payment = new PaymentDTO();
        payment.setId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
        payment.setTransactionId("TX-1");
        payment.setAmount(new BigDecimal("10.00"));
        payment.setCurrency("BRL");
        payment.setPaymentMethod(PaymentMethod.PIX);
        payment.setStatus(PaymentStatus.PENDING);
        payment.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        payment.setModifiedAt(Instant.parse("2026-01-02T00:00:00Z"));
        return payment;
    }

    public static PaymentEntity paymentEntity() {
        return PaymentMapper.toEntity(paymentDto());
    }
}
