package com.dulce.backend.notification;

import com.dulce.backend.catalog.CakeFinish;
import com.dulce.backend.customer.Customer;
import com.dulce.backend.order.CakeOrder;
import com.dulce.backend.order.CakeOrderRepository;
import com.dulce.backend.order.OrderItem;
import com.dulce.backend.order.OrderItemRepository;
import com.dulce.backend.order.OrderStatus;
import com.dulce.backend.payment.Invoice;
import com.dulce.backend.payment.InvoiceRepository;
import com.dulce.backend.payment.InvoiceStatus;
import com.dulce.backend.payment.Payment;
import com.dulce.backend.payment.PaymentRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
public class NotificationVariablesBuilder {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    private final CakeOrderRepository cakeOrderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    public NotificationVariablesBuilder(
            CakeOrderRepository cakeOrderRepository,
            OrderItemRepository orderItemRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository) {
        this.cakeOrderRepository = cakeOrderRepository;
        this.orderItemRepository = orderItemRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
    }

    public Map<String, String> buildForOrder(Long orderId) {
        CakeOrder order =
                cakeOrderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Pedido não encontrado para notificação: "
                                                        + orderId));
        Customer customer = order.getCustomer();
        List<OrderItem> items = orderItemRepository.findByCakeOrderId(orderId);
        Invoice invoice = invoiceRepository.findByCakeOrderId(orderId).orElse(null);
        List<Payment> payments =
                invoice == null ? List.of() : paymentRepository.findByInvoiceId(invoice.getId());

        BigDecimal gross = invoice != null ? invoice.getGrossAmount() : BigDecimal.ZERO;
        BigDecimal discount = invoice != null ? invoice.getDiscount() : BigDecimal.ZERO;
        BigDecimal total = gross.subtract(discount);
        BigDecimal paidSoFar =
                payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        ZonedDateTime pickupLocal = order.getPickupAt().atZoneSameInstant(BUSINESS_ZONE);

        boolean refundApplicable =
                order.getStatus() == OrderStatus.CANCELED
                        || order.getStatus() == OrderStatus.REJECTED;

        Map<String, String> variables = new HashMap<>();
        variables.put("customerName", escape(customer.getName()));
        variables.put("orderId", String.valueOf(order.getId()));
        variables.put("orderStatus", translateOrderStatus(order.getStatus()));
        variables.put("pickupDate", pickupLocal.format(DATE_FORMAT));
        variables.put("pickupTime", pickupLocal.format(TIME_FORMAT));
        variables.put("orderTotal", formatCurrency(total));
        variables.put("orderItems", buildItemsHtml(items));
        variables.put(
                "discountNotice",
                discount.compareTo(BigDecimal.ZERO) > 0
                        ? "Desconto aplicado: " + formatCurrency(discount) + "."
                        : "");
        variables.put(
                "orderNotesNotice",
                (order.getNotes() != null && !order.getNotes().isBlank())
                        ? "<p><strong>Observações do pedido:</strong> "
                                + escape(order.getNotes())
                                + "</p>"
                        : "");
        variables.put(
                "refundNotice",
                (refundApplicable && paidSoFar.compareTo(BigDecimal.ZERO) > 0)
                        ? "Valor a devolver: " + formatCurrency(paidSoFar) + "."
                        : "");
        variables.put(
                "paymentStatus",
                invoice != null ? translateInvoiceStatus(invoice.getStatus()) : "Pendente");
        variables.put("amountPaid", formatCurrency(paidSoFar));

        return variables;
    }

    private String buildItemsHtml(List<OrderItem> items) {
        StringBuilder html = new StringBuilder("<ul>");
        for (OrderItem item : items) {
            html.append("<li><strong>")
                    .append(escape(item.getFlavor().getName()))
                    .append(" — ")
                    .append(escape(item.getSize().getName()))
                    .append("</strong><br>Massa: ")
                    .append(translateFinish(item.getCakeBase()))
                    .append(" &middot; Raspa: ")
                    .append(translateFinish(item.getTopping()));
            if (item.getMessage() != null && !item.getMessage().isBlank()) {
                html.append("<br>Mensagem no bolo: \"")
                        .append(escape(item.getMessage()))
                        .append("\"");
            }
            if (item.getNotes() != null && !item.getNotes().isBlank()) {
                html.append("<br>Observações: ").append(escape(item.getNotes()));
            }
            html.append("<br>Valor: ").append(formatCurrency(item.getUnitPrice()));
            html.append("</li>");
        }
        html.append("</ul>");
        return html.toString();
    }

    private String translateOrderStatus(OrderStatus status) {
        return switch (status) {
            case PENDING -> "Pendente";
            case ACCEPTED -> "Aceito";
            case REJECTED -> "Recusado";
            case CANCELED -> "Cancelado";
            case COMPLETED -> "Concluído";
        };
    }

    private String translateInvoiceStatus(InvoiceStatus status) {
        return switch (status) {
            case PENDING -> "Pendente";
            case PARTIAL -> "Parcial";
            case PAID -> "Pago";
        };
    }

    private String translateFinish(CakeFinish finish) {
        return finish == CakeFinish.white ? "Branca" : "Preta";
    }

    private String formatCurrency(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(value);
    }

    private String escape(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }
}
