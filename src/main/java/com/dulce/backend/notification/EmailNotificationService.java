package com.dulce.backend.notification;

import com.dulce.backend.order.CakeOrder;
import com.dulce.backend.order.CakeOrderRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final String fromAddress;
    private final NotificationTypeRepository notificationTypeRepository;
    private final CakeOrderRepository cakeOrderRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationVariablesBuilder variablesBuilder;
    private final TemplateHtmlSanitizer sanitizer;

    public EmailNotificationService(
            JavaMailSender mailSender,
            TemplateEngine templateEngine,
            @Value("${app.mail.from}") String fromAddress,
            NotificationTypeRepository notificationTypeRepository,
            CakeOrderRepository cakeOrderRepository,
            NotificationHistoryRepository notificationHistoryRepository,
            NotificationVariablesBuilder variablesBuilder,
            TemplateHtmlSanitizer sanitizer) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.fromAddress = fromAddress;
        this.notificationTypeRepository = notificationTypeRepository;
        this.cakeOrderRepository = cakeOrderRepository;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.variablesBuilder = variablesBuilder;
        this.sanitizer = sanitizer;
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderCreated(Long orderId) {
        sendOrderNotification(NotificationTypeCode.ORDER_CREATED, orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderUpdated(Long orderId) {
        sendOrderNotification(NotificationTypeCode.ORDER_UPDATED, orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderAccepted(Long orderId) {
        sendOrderNotification(NotificationTypeCode.ORDER_ACCEPTED, orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderRejected(Long orderId) {
        sendOrderNotification(NotificationTypeCode.ORDER_REJECTED, orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderCanceled(Long orderId) {
        sendOrderNotification(NotificationTypeCode.ORDER_CANCELED, orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyOrderCompleted(Long orderId) {
        sendOrderNotification(NotificationTypeCode.ORDER_COMPLETED, orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPaymentUpdated(Long orderId) {
        sendOrderNotification(NotificationTypeCode.PAYMENT_UPDATED, orderId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyPickupReminder(Long orderId) {
        sendOrderNotification(NotificationTypeCode.PICKUP_REMINDER, orderId);
    }

    private void sendOrderNotification(NotificationTypeCode code, Long orderId) {
        try {
            CakeOrder order =
                    cakeOrderRepository
                            .findById(orderId)
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Pedido não encontrado: " + orderId));

            if (order.getCustomer().getEmail() == null) {
                log.info(
                        "Cliente {} não tem e-mail cadastrado — notificação {} não enviada.",
                        order.getCustomer().getId(),
                        code);
                return;
            }

            NotificationType type =
                    notificationTypeRepository
                            .findByCode(code.name())
                            .orElseThrow(
                                    () ->
                                            new IllegalStateException(
                                                    "Template de notificação não encontrado: "
                                                            + code));

            Map<String, String> variables = variablesBuilder.buildForOrder(orderId);
            String subject =
                    TemplateSubstitution.apply(type.getSubject(), variables)
                            .replaceAll("[\r\n]+", " ");
            String bodyHtml =
                    sanitizer.sanitize(
                            TemplateSubstitution.apply(type.getTemplateContent(), variables));

            send(
                    order.getCustomer().getEmail(),
                    subject,
                    "email/notification-shell",
                    Map.of("subject", subject, "bodyHtml", bodyHtml));

            NotificationHistory history = new NotificationHistory();
            history.setNotificationType(type);
            history.setCakeOrder(order);
            history.setCustomer(order.getCustomer());
            history.setEmail(order.getCustomer().getEmail());
            history.setContent(bodyHtml);
            history.setSentAt(OffsetDateTime.now());
            notificationHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("Falha ao enviar notificação {} para o pedido {}.", code, orderId, e);
        }
    }
}
