package com.dulce.backend.notification;

public interface OutboundEmailSender {

    void send(String to, String subject, String html);
}
