package com.dulce.backend.ticket;

import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.common.exception.ResourceNotFoundException;
import com.dulce.backend.report.ReportRange;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class TicketService {

    static final long MAX_DAYS = 14;
    static final int MAX_TICKETS = 150;
    private static final int COLUMNS = 3;
    private static final String[] WEEKDAYS = {"seg", "ter", "qua", "qui", "sex", "sáb", "dom"};
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("dd/MM 'às' HH:mm");

    private static final String ROWS_SQL =
            """
            SELECT co.id AS order_id, co.customer_id AS customer_id,
                   c.name AS customer_name, c.phone AS customer_phone,
                   co.pickup_at AS pickup_at, co.status AS order_status, co.notes AS order_notes,
                   f.name AS flavor_name, s.name AS size_name,
                   oi.cake_base AS cake_base, oi.topping AS topping,
                   oi.message AS message, oi.notes AS item_notes,
                   (inv.gross_amount - inv.discount) AS due,
                   COALESCE((SELECT SUM(p.amount) FROM payment p WHERE p.invoice_id = inv.id), 0)
                       AS received
            FROM cake_order co
            JOIN customer c ON c.id = co.customer_id
            JOIN invoice inv ON inv.cake_order_id = co.id
            JOIN order_item oi ON oi.cake_order_id = co.id
            JOIN flavor f ON f.id = oi.flavor_id
            JOIN size s ON s.id = oi.size_id
            WHERE co.status IN ('ACCEPTED', 'COMPLETED')
              AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            ORDER BY co.pickup_at ASC, co.id ASC, oi.id ASC
            """;

    private static final String SUMMARY_SQL =
            """
            SELECT COUNT(DISTINCT co.id) AS orders_count, COUNT(oi.id) AS tickets_count
            FROM cake_order co
            JOIN order_item oi ON oi.cake_order_id = co.id
            WHERE co.status IN ('ACCEPTED', 'COMPLETED')
              AND co.pickup_at >= :start AND co.pickup_at < :endExclusive
            """;

    private record Row(
            long orderId,
            long customerId,
            String customerName,
            String customerPhone,
            OffsetDateTime pickupAt,
            String orderStatus,
            String orderNotes,
            String flavorName,
            String sizeName,
            String cakeBase,
            String topping,
            String message,
            String itemNotes,
            BigDecimal due,
            BigDecimal received) {}

    private final JdbcClient jdbcClient;
    private final TemplateEngine templateEngine;
    private final TicketPdfRenderer pdfRenderer;

    public TicketService(
            JdbcClient jdbcClient, TemplateEngine templateEngine, TicketPdfRenderer pdfRenderer) {
        this.jdbcClient = jdbcClient;
        this.templateEngine = templateEngine;
        this.pdfRenderer = pdfRenderer;
    }

    @Transactional(readOnly = true)
    public TicketSummaryResponse summary(LocalDate from, LocalDate to) {
        ReportRange range = ReportRange.resolve(from, to, MAX_DAYS);
        return jdbcClient
                .sql(SUMMARY_SQL)
                .param("start", range.start())
                .param("endExclusive", range.endExclusive())
                .query(
                        (rs, rowNum) ->
                                new TicketSummaryResponse(
                                        range.from(),
                                        range.to(),
                                        rs.getLong("orders_count"),
                                        rs.getLong("tickets_count"),
                                        MAX_TICKETS))
                .single();
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(LocalDate from, LocalDate to) {
        ReportRange range = ReportRange.resolve(from, to, MAX_DAYS);
        List<Row> rows = fetchRows(range);

        if (rows.isEmpty()) {
            throw new ResourceNotFoundException(
                    "Nenhum pedido encontrado para o período informado.");
        }
        if (rows.size() > MAX_TICKETS) {
            throw new BadRequestException(
                    "Muitos tickets para imprimir de uma vez (máx. "
                            + MAX_TICKETS
                            + "). Reduza o período.");
        }

        Map<Long, Long> itemsPerOrder =
                rows.stream().collect(Collectors.groupingBy(Row::orderId, Collectors.counting()));
        Map<Long, Integer> seen = new HashMap<>();
        List<TicketView> tickets = new ArrayList<>();
        for (Row row : rows) {
            int index = seen.merge(row.orderId(), 1, Integer::sum);
            tickets.add(toView(row, index, itemsPerOrder.get(row.orderId()).intValue()));
        }

        List<List<TicketView>> grid = new ArrayList<>();
        for (int i = 0; i < tickets.size(); i += COLUMNS) {
            List<TicketView> line =
                    new ArrayList<>(tickets.subList(i, Math.min(i + COLUMNS, tickets.size())));
            while (line.size() < COLUMNS) {
                line.add(null);
            }
            grid.add(line);
        }

        Context context = new Context();
        context.setVariable("rows", grid);
        String html = templateEngine.process("print/order-tickets", context);
        return pdfRenderer.render(html);
    }

    private List<Row> fetchRows(ReportRange range) {
        return jdbcClient
                .sql(ROWS_SQL)
                .param("start", range.start())
                .param("endExclusive", range.endExclusive())
                .query(
                        (rs, rowNum) ->
                                new Row(
                                        rs.getLong("order_id"),
                                        rs.getLong("customer_id"),
                                        rs.getString("customer_name"),
                                        rs.getString("customer_phone"),
                                        rs.getObject("pickup_at", OffsetDateTime.class),
                                        rs.getString("order_status"),
                                        rs.getString("order_notes"),
                                        rs.getString("flavor_name"),
                                        rs.getString("size_name"),
                                        rs.getString("cake_base"),
                                        rs.getString("topping"),
                                        rs.getString("message"),
                                        rs.getString("item_notes"),
                                        rs.getBigDecimal("due"),
                                        rs.getBigDecimal("received")))
                .list();
    }

    private TicketView toView(Row row, int index, int total) {
        BigDecimal missing = row.due().subtract(row.received()).max(BigDecimal.ZERO);
        boolean open = missing.signum() > 0;
        String paymentLabel;
        if (!open) {
            paymentLabel = "Pago";
        } else if (row.received().signum() <= 0) {
            paymentLabel = "Pendente · falta " + money(missing);
        } else {
            paymentLabel = "Parcial · falta " + money(missing);
        }

        String message = PdfText.clean(row.message());
        ZonedDateTime pickup = row.pickupAt().atZoneSameInstant(ReportRange.BUSINESS_ZONE);

        return new TicketView(
                "Pedido #" + row.orderId() + " · Cliente #" + row.customerId(),
                "Bolo " + index + " de " + total,
                "COMPLETED".equals(row.orderStatus()),
                PdfText.orEmpty(row.customerName()),
                formatPhone(row.customerPhone()),
                PdfText.orEmpty(row.sizeName()) + " " + PdfText.orEmpty(row.flavorName()),
                "– Massa " + finish(row.cakeBase()),
                "– Raspa " + finish(row.topping()),
                message == null ? null : "– “" + message + "”",
                PdfText.clean(row.itemNotes()),
                paymentLabel,
                open,
                PdfText.clean(row.orderNotes()),
                "Retirada: "
                        + WEEKDAYS[pickup.getDayOfWeek().getValue() - 1]
                        + ", "
                        + pickup.format(DATE_TIME));
    }

    private String finish(String finish) {
        return "white".equals(finish) ? "branca" : "preta";
    }

    private String money(BigDecimal value) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(value);
    }

    private String formatPhone(String phone) {
        if (phone == null) {
            return "";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return "("
                    + digits.substring(0, 2)
                    + ") "
                    + digits.substring(2, 7)
                    + "-"
                    + digits.substring(7);
        }
        if (digits.length() == 10) {
            return "("
                    + digits.substring(0, 2)
                    + ") "
                    + digits.substring(2, 6)
                    + "-"
                    + digits.substring(6);
        }
        return PdfText.orEmpty(phone);
    }
}
