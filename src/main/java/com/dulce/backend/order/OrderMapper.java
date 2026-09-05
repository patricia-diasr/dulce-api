package com.dulce.backend.order;

import com.dulce.backend.order.dto.OrderItemResponse;
import com.dulce.backend.order.dto.OrderResponse;
import com.dulce.backend.order.dto.OrderSummaryResponse;
import com.dulce.backend.payment.Invoice;
import com.dulce.backend.payment.InvoiceRepository;
import com.dulce.backend.payment.Payment;
import com.dulce.backend.payment.PaymentRepository;
import com.dulce.backend.payment.dto.InvoiceResponse;
import com.dulce.backend.payment.dto.PaymentResponse;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    private final OrderItemRepository orderItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public OrderMapper(
            OrderItemRepository orderItemRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository) {
        this.orderItemRepository = orderItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public OrderResponse toResponse(CakeOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getCustomer().getName(),
                order.getCreatedAt(),
                order.getPickupAt(),
                order.getCompletedAt(),
                order.getStatus(),
                order.getCreationChannel(),
                order.getNotes(),
                toItemResponses(order.getId()),
                toInvoiceResponseOrNull(order.getId()));
    }

    public OrderSummaryResponse toSummary(CakeOrder order) {
        return new OrderSummaryResponse(
                order.getId(),
                order.getCreatedAt(),
                order.getPickupAt(),
                order.getCompletedAt(),
                order.getStatus(),
                order.getCreationChannel(),
                order.getNotes(),
                toItemResponses(order.getId()),
                toInvoiceResponseOrNull(order.getId()));
    }

    public InvoiceResponse toInvoiceResponse(Invoice invoice) {
        List<PaymentResponse> payments =
                paymentRepository.findByInvoiceId(invoice.getId()).stream()
                        .map(this::toPaymentResponse)
                        .toList();

        return new InvoiceResponse(
                invoice.getId(),
                invoice.getGrossAmount(),
                invoice.getDiscount(),
                invoice.getStatus(),
                computeRefundDue(invoice, payments),
                payments);
    }

    private InvoiceResponse toInvoiceResponseOrNull(Long orderId) {
        return invoiceRepository
                .findByCakeOrderId(orderId)
                .map(this::toInvoiceResponse)
                .orElse(null);
    }

    private List<OrderItemResponse> toItemResponses(Long orderId) {
        return orderItemRepository.findByCakeOrderId(orderId).stream()
                .map(
                        item ->
                                new OrderItemResponse(
                                        item.getId(),
                                        item.getFlavor().getId(),
                                        item.getFlavor().getName(),
                                        item.getSize().getId(),
                                        item.getSize().getName(),
                                        item.getTopping(),
                                        item.getCakeBase(),
                                        item.getUnitPrice(),
                                        item.getMessage(),
                                        item.getNotes()))
                .toList();
    }

    private PaymentResponse toPaymentResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getAmount(),
                payment.getPaidAt(),
                payment.getPaymentMethod());
    }

    private BigDecimal computeRefundDue(Invoice invoice, List<PaymentResponse> payments) {
        OrderStatus status = invoice.getCakeOrder().getStatus();

        if (status != OrderStatus.CANCELED && status != OrderStatus.REJECTED) {
            return BigDecimal.ZERO;
        }

        return payments.stream()
                .map(PaymentResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
