package com.dulce.backend.ticket;

import java.time.LocalDate;

public record TicketSummaryResponse(
        LocalDate from, LocalDate to, long ordersCount, long ticketsCount, int maxTickets) {}
