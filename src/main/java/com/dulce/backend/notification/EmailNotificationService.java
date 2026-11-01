package com.dulce.backend.notification;

import com.dulce.backend.order.CakeOrder;
import com.dulce.backend.order.CakeOrderRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class EmailNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    private final OutboundEmailSender emailSender;
    private final TemplateEngine templateEngine;
    private final NotificationTypeRepository notificationTypeRepository;
    private final CakeOrderRepository cakeOrderRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationVariablesBuilder variablesBuilder;
    private final TemplateHtmlSanitizer sanitizer;
    private final TransactionTemplate newTransaction;

    public EmailNotificationService(
            OutboundEmailSender emailSender,
            TemplateEngine templateEngine,
            NotificationTypeRepository notificationTypeRepository,
            CakeOrderRepository cakeOrderRepository,
            NotificationHistoryRepository notificationHistoryRepository,
            NotificationVariablesBuilder variablesBuilder,
            TemplateHtmlSanitizer sanitizer,
            PlatformTransactionManager transactionManager) {
        this.emailSender = emailSender;
        this.templateEngine = templateEngine;
        this.notificationTypeRepository = notificationTypeRepository;
        this.cakeOrderRepository = cakeOrderRepository;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.variablesBuilder = variablesBuilder;
        this.sanitizer = sanitizer;
        this.newTransaction = new TransactionTemplate(transactionManager);
        this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Override
    public void send(String to, String subject, String templateName, Map<String, Object> variables) {
        Context context = new Context();
        context.setVariables(variables);
        String html = templateEngine.process(templateName, context);

        emailSender.send(to, subject, html);
    }

    @Override
    public void notifyOrderCreated(Long orderId) {
        dispatch(NotificationTypeCode.ORDER_CREATED, orderId);
    }

    @Override
    public void notifyOrderUpdated(Long orderId) {
        dispatch(NotificationTypeCode.ORDER_UPDATED, orderId);
    }

    @Override
    public void notifyOrderAccepted(Long orderId) {
        dispatch(NotificationTypeCode.ORDER_ACCEPTED, orderId);
    }

    @Override
    public void notifyOrderRejected(Long orderId) {
        dispatch(NotificationTypeCode.ORDER_REJECTED, orderId);
    }

    @Override
    public void notifyOrderCanceled(Long orderId) {
        dispatch(NotificationTypeCode.ORDER_CANCELED, orderId);
    }

    @Override
    public void notifyOrderCompleted(Long orderId) {
        dispatch(NotificationTypeCode.ORDER_COMPLETED, orderId);
    }

    @Override
    public void notifyPaymentUpdated(Long orderId) {
        dispatch(NotificationTypeCode.PAYMENT_UPDATED, orderId);
    }

    @Override
    public void notifyPickupReminder(Long orderId) {
        dispatch(NotificationTypeCode.PICKUP_REMINDER, orderId);
    }

    private void dispatch(NotificationTypeCode code, Long orderId) {
        Runnable task =
                () -> {
                    try {
                        newTransaction.executeWithoutResult(status -> sendOrderNotification(code, orderId));
                    } catch (Exception e) {
                        log.error("Falha ao enviar notificação {} para o pedido {}.", code, orderId, e);
                    }
                };

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            task.run();
                        }
                    });
        } else {
            task.run();
        }
    }

    private void sendOrderNotification(NotificationTypeCode code, Long orderId) {
        CakeOrder order =
                cakeOrderRepository
                        .findById(orderId)
                        .orElseThrow(() -> new IllegalStateException("Pedido não encontrado: " + orderId));

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
                                () -> new IllegalStateException("Template de notificação não encontrado: " + code));

        Map<String, String> variables = variablesBuilder.buildForOrder(orderId);
        String subject =
                TemplateSubstitution.apply(type.getSubject(), variables).replaceAll("[\r\n]+", " ");
        String bodyHtml =
                sanitizer.sanitize(TemplateSubstitution.apply(type.getTemplateContent(), variables));

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
    }
}
