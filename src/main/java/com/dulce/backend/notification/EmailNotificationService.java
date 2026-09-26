package com.dulce.backend.notification;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;

    public EmailNotificationService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${app.mail.from}") String fromAddress) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
    }

    @Override
    public void send(
            String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String html = templateEngine.process(templateName, context);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper =
                    new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
            helper.setTo(to);
            helper.setFrom(fromAddress);
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(message);
        } catch (MessagingException e) {
            throw new EmailSendingException("Falha ao enviar e-mail para " + to, e);
        }
    }

    @Override
    public void notifyOrderCreated(Long orderId) {
        log.info("[placeholder] Notificação: pedido criado (orderId={})", orderId);
    }

    @Override
    public void notifyOrderUpdated(Long orderId) {
        log.info("[placeholder] Notificação: pedido editado (orderId={})", orderId);
    }

    @Override
    public void notifyOrderAccepted(Long orderId) {
        log.info("[placeholder] Notificação: pedido aceito (orderId={})", orderId);
    }

    @Override
    public void notifyOrderRejected(Long orderId) {
        log.info("[placeholder] Notificação: pedido recusado (orderId={})", orderId);
    }

    @Override
    public void notifyOrderCanceled(Long orderId) {
        log.info("[placeholder] Notificação: pedido cancelado (orderId={})", orderId);
    }

    @Override
    public void notifyOrderCompleted(Long orderId) {
        log.info("[placeholder] Notificação: pedido concluído (orderId={})", orderId);
    }

    @Override
    public void notifyPaymentRegistered(Long orderId, Long paymentId) {
        log.info(
                "[placeholder] Notificação: pagamento registrado (orderId={}, paymentId={})",
                orderId,
                paymentId);
    }
}
