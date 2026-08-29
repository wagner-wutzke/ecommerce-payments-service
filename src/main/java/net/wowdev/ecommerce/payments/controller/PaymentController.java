package net.wowdev.ecommerce.payments.controller;

import jakarta.validation.Valid;
import net.wowdev.ecommerce.domain.dto.PaymentDTO;
import net.wowdev.ecommerce.payments.service.PaymentNotFoundException;
import net.wowdev.ecommerce.payments.service.PaymentService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {
    private final PaymentService service;

    public PaymentController(final PaymentService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public PaymentDTO get(@PathVariable final UUID id) {
        return service.findById(id);
    }

    @GetMapping
    public Page<PaymentDTO> list(@RequestParam(defaultValue = "0") final int page,
                                 @RequestParam(defaultValue = "20") final int pageSize) {
        if (page < 0 || pageSize < 1 || pageSize > 100) {
            throw new IllegalArgumentException("page must be non-negative and pageSize must be between 1 and 100");
        }
        return service.findAll(PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping
    public ResponseEntity<PaymentDTO> create(@Valid @RequestBody final PaymentDTO payment) {
        final PaymentDTO created = service.create(payment);
        return ResponseEntity.created(URI.create("/api/v1/payments/" + created.getId())).body(created);
    }

    @PutMapping("/{id}")
    public PaymentDTO update(@PathVariable final UUID id, @Valid @RequestBody final PaymentDTO payment) {
        return service.update(id, payment);
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
//        service.delete(id);
//        return ResponseEntity.noContent().build();
//    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<Void> notFound(final PaymentNotFoundException exception) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> badRequest(final IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(exception.getMessage());
    }
}
