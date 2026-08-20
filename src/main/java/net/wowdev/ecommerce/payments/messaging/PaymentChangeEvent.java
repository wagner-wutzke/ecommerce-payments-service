package net.wowdev.ecommerce.payments.messaging;

import net.wowdev.ecommerce.domain.dto.PaymentDTO;

public record PaymentChangeEvent(String eventType, PaymentDTO payload) {
}
