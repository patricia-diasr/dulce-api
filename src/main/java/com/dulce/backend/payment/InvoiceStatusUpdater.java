package com.dulce.backend.payment;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class InvoiceStatusUpdater {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public InvoiceStatusUpdater(
            InvoiceRepository invoiceRepository, PaymentRepository paymentRepository) {
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public void recalculate(Invoice invoice) {
        BigDecimal paid =
                paymentRepository.findByInvoiceId(invoice.getId()).stream()
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal due = invoice.getGrossAmount().subtract(invoice.getDiscount());

        if (paid.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PENDING);
        } else if (paid.compareTo(due) >= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else {
            invoice.setStatus(InvoiceStatus.PARTIAL);
        }

        invoiceRepository.save(invoice);
    }
}
