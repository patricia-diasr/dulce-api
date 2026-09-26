package com.dulce.backend.payment;

import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.common.exception.InvalidOrderStateException;
import com.dulce.backend.common.exception.ResourceNotFoundException;
import com.dulce.backend.notification.NotificationService;
import com.dulce.backend.order.CakeOrder;
import com.dulce.backend.order.CakeOrderRepository;
import com.dulce.backend.order.OrderMapper;
import com.dulce.backend.order.OrderStatus;
import com.dulce.backend.payment.dto.InvoiceResponse;
import com.dulce.backend.payment.dto.PaymentRequest;
import com.dulce.backend.payment.dto.PaymentUpdateRequest;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentService {

    private final CakeOrderRepository cakeOrderRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final OrderMapper orderMapper;
    private final NotificationService notificationService;

    public PaymentService(
            CakeOrderRepository cakeOrderRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            OrderMapper orderMapper,
            NotificationService notificationService) {
        this.cakeOrderRepository = cakeOrderRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.orderMapper = orderMapper;
        this.notificationService = notificationService;
    }

    @Transactional
    public InvoiceResponse register(Long orderId, PaymentRequest request) {
        CakeOrder order = findOrderOrThrow(orderId);

        if (order.getStatus() != OrderStatus.ACCEPTED
                && order.getStatus() != OrderStatus.COMPLETED) {
            throw new InvalidOrderStateException(
                    "Não é possível registrar pagamento nesse pedido.");
        }

        Invoice invoice = findInvoiceOrThrow(orderId);

        if (request.amount().compareTo(remainingBalance(invoice, null)) > 0) {
            throw new BadRequestException("Valor excede o saldo restante do pedido.");
        }

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setAmount(request.amount());
        payment.setPaidAt(OffsetDateTime.now());
        payment.setPaymentMethod(request.paymentMethod());
        payment = paymentRepository.save(payment);

        recalculateInvoiceStatus(invoice);
        notificationService.notifyPaymentRegistered(order.getId(), payment.getId());

        return orderMapper.toInvoiceResponse(invoice);
    }

    @Transactional
    public InvoiceResponse update(Long orderId, Long paymentId, PaymentUpdateRequest request) {
        Invoice invoice = findInvoiceOrThrow(orderId);
        Payment payment = findPaymentOrThrow(invoice, paymentId);

        if (request.amount() != null) {
            if (request.amount().compareTo(remainingBalance(invoice, payment.getId())) > 0) {
                throw new BadRequestException("Valor excede o saldo restante do pedido.");
            }

            payment.setAmount(request.amount());
        }

        if (request.paymentMethod() != null) {
            payment.setPaymentMethod(request.paymentMethod());
        }

        paymentRepository.save(payment);
        recalculateInvoiceStatus(invoice);

        return orderMapper.toInvoiceResponse(invoice);
    }

    @Transactional
    public void delete(Long orderId, Long paymentId) {
        Invoice invoice = findInvoiceOrThrow(orderId);
        Payment payment = findPaymentOrThrow(invoice, paymentId);

        paymentRepository.delete(payment);
        recalculateInvoiceStatus(invoice);
    }

    private CakeOrder findOrderOrThrow(Long orderId) {
        return cakeOrderRepository
                .findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido não encontrado."));
    }

    private Invoice findInvoiceOrThrow(Long orderId) {
        if (!cakeOrderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Pedido não encontrado.");
        }

        return invoiceRepository
                .findByCakeOrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("Pedido sem fatura associada."));
    }

    private Payment findPaymentOrThrow(Invoice invoice, Long paymentId) {
        return paymentRepository
                .findById(paymentId)
                .filter(p -> p.getInvoice().getId().equals(invoice.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado."));
    }

    private BigDecimal remainingBalance(Invoice invoice, Long excludingPaymentId) {
        BigDecimal paid =
                paymentRepository.findByInvoiceId(invoice.getId()).stream()
                        .filter(
                                p ->
                                        excludingPaymentId == null
                                                || !p.getId().equals(excludingPaymentId))
                        .map(Payment::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        return invoice.getGrossAmount().subtract(invoice.getDiscount()).subtract(paid);
    }

    private void recalculateInvoiceStatus(Invoice invoice) {
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
