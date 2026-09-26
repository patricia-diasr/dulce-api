package com.dulce.backend.payment;

import com.dulce.backend.payment.dto.InvoiceResponse;
import com.dulce.backend.payment.dto.PaymentRequest;
import com.dulce.backend.payment.dto.PaymentUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/orders/{orderId}/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ResponseEntity<InvoiceResponse> register(
            @PathVariable Long orderId, @Valid @RequestBody PaymentRequest request) {
        InvoiceResponse response = paymentService.register(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{paymentId}")
    public InvoiceResponse update(
            @PathVariable Long orderId,
            @PathVariable Long paymentId,
            @Valid @RequestBody PaymentUpdateRequest request) {
        return paymentService.update(orderId, paymentId, request);
    }

    @DeleteMapping("/{paymentId}")
    public ResponseEntity<Void> delete(@PathVariable Long orderId, @PathVariable Long paymentId) {
        paymentService.delete(orderId, paymentId);
        return ResponseEntity.noContent().build();
    }
}
