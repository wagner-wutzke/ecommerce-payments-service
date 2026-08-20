package net.wowdev.ecommerce.payments.service;

import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.domain.entity.PaymentEntity;
import net.wowdev.ecommerce.domain.mapper.PaymentMapper;
import net.wowdev.ecommerce.payments.messaging.PaymentChangeEvent;
import net.wowdev.ecommerce.payments.repository.PaymentRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DefaultPaymentService implements PaymentService {
    private final PaymentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    public DefaultPaymentService(final PaymentRepository repository, final ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentDTO findById(final UUID id) {
        return repository.findById(id).map(PaymentMapper::toDto)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentDTO> findAll(final Pageable pageable) {
        return repository.findAll(pageable).map(PaymentMapper::toDto);
    }

    @Override
    @Transactional
    public PaymentDTO create(final PaymentDTO payment) {
        final PaymentEntity saved = repository.save(PaymentMapper.toEntity(payment));
        final PaymentDTO result = PaymentMapper.toDto(saved);
        eventPublisher.publishEvent(new PaymentChangeEvent("CREATE", result));
        return result;
    }

    @Override
    @Transactional
    public PaymentDTO update(final UUID id, final PaymentDTO payment) {
        final PaymentEntity current = repository.findById(id)
                .orElseThrow(() -> new PaymentNotFoundException("Payment not found: " + id));
        final PaymentDTO replacement = PaymentMapper.toDto(current);
        replacement.setTransactionId(payment.getTransactionId());
        replacement.setAmount(payment.getAmount());
        replacement.setCurrency(payment.getCurrency());
        replacement.setPaymentMethod(payment.getPaymentMethod());
        replacement.setStatus(payment.getStatus());
        final PaymentDTO result = PaymentMapper.toDto(repository.save(PaymentMapper.toEntity(replacement)));
        eventPublisher.publishEvent(new PaymentChangeEvent("UPDATE", result));
        return result;
    }

    @Override
    @Transactional
    public void delete(final UUID id) {
        if (!repository.existsById(id)) {
            throw new PaymentNotFoundException("Payment not found: " + id);
        }
        repository.deleteById(id);
    }
}
