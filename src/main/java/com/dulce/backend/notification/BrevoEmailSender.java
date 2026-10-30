package com.dulce.backend.notification;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "brevo")
public class BrevoEmailSender implements OutboundEmailSender {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;
    private final String senderName;
    private final String replyToAddress;

    public BrevoEmailSender(
            @Value("${app.mail.brevo.api-key}") String apiKey,
            @Value("${app.mail.from}") String fromAddress,
            @Value("${app.mail.sender-name:Dulce}") String senderName,
            @Value("${app.mail.reply-to:}") String replyToAddress) {
        this.restClient = RestClient.create("https://api.brevo.com");
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
        this.senderName = senderName;
        this.replyToAddress = replyToAddress;
    }

    @Override
    public void send(String to, String subject, String html) {
        Map<String, Object> body =
                new HashMap<>(
                        Map.of(
                                "sender", Map.of("name", senderName, "email", fromAddress),
                                "to", List.of(Map.of("email", to)),
                                "subject", subject,
                                "htmlContent", html));

        if (!replyToAddress.isBlank()) {
            body.put("replyTo", Map.of("email", replyToAddress));
        }

        try {
            restClient
                    .post()
                    .uri("/v3/smtp/email")
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new EmailSendingException("Falha ao enviar e-mail para " + to + " via Brevo", e);
        }
    }
}
