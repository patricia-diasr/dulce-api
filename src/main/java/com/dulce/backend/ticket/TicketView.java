package com.dulce.backend.ticket;

public record TicketView(
        String headline,
        String cakeCounter,
        boolean completed,
        String customerName,
        String customerPhone,
        String title,
        String massLine,
        String toppingLine,
        String messageLine,
        String itemNotes,
        String paymentLabel,
        boolean paymentOpen,
        String orderNotes,
        String pickupLabel) {}
