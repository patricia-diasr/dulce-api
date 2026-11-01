package com.dulce.backend.notification;

import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(prefix = "app.mail", name = "provider", havingValue = "resend")
public class ResendEmailSender implements OutboundEmailSender {

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;

    public ResendEmailSender(
            @Value("${app.mail.resend.api-key}") String apiKey,
            @Value("${app.mail.from}") String fromAddress) {
        this.restClient = RestClient.create("https://api.resend.com");
        this.apiKey = apiKey;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(String to, String subject, String html) {
        try {
            restClient
                    .post()
                    .uri("/emails")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("from", fromAddress, "to", to, "subject", subject, "html", html))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            throw new EmailSendingException("Falha ao enviar e-mail para " + to + " via Resend", e);
        }
    }
}
