package com.dulce.backend.notification;

import com.dulce.backend.common.exception.BadRequestException;
import com.dulce.backend.common.exception.ResourceNotFoundException;
import com.dulce.backend.notification.dto.NotificationTemplatePreviewRequest;
import com.dulce.backend.notification.dto.NotificationTemplatePreviewResponse;
import com.dulce.backend.notification.dto.NotificationTemplateResponse;
import com.dulce.backend.notification.dto.NotificationTemplateUpdateRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class NotificationTemplateService {

    private final NotificationTypeRepository notificationTypeRepository;
    private final TemplateHtmlSanitizer sanitizer;
    private final TemplateVariableValidator variableValidator;
    private final TemplateEngine templateEngine;

    public NotificationTemplateService(
            NotificationTypeRepository notificationTypeRepository,
            TemplateHtmlSanitizer sanitizer,
            TemplateVariableValidator variableValidator,
            TemplateEngine templateEngine) {
        this.notificationTypeRepository = notificationTypeRepository;
        this.sanitizer = sanitizer;
        this.variableValidator = variableValidator;
        this.templateEngine = templateEngine;
    }

    @Transactional(readOnly = true)
    public List<NotificationTemplateResponse> list() {
        return notificationTypeRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public NotificationTemplateResponse getById(Long id) {
        return toResponse(findOrThrow(id));
    }

    @Transactional
    public NotificationTemplateResponse update(Long id, NotificationTemplateUpdateRequest request) {
        NotificationType type = findOrThrow(id);

        if (request.subject() != null) {
            if (request.subject().isBlank()) {
                throw new BadRequestException("Assunto é obrigatório.");
            }

            variableValidator.validate(request.subject());
            type.setSubject(request.subject());
        }

        if (request.content() != null) {
            if (request.content().isBlank()) {
                throw new BadRequestException("Conteúdo é obrigatório.");
            }

            variableValidator.validate(request.content());
            type.setTemplateContent(request.content());
        }

        return toResponse(type);
    }

    @Transactional(readOnly = true)
    public NotificationTemplatePreviewResponse preview(NotificationTemplatePreviewRequest request) {
        variableValidator.validate(request.subject());
        variableValidator.validate(request.content());

        Map<String, String> sampleVariables = sampleVariables();
        String subject =
                TemplateSubstitution.apply(request.subject(), sampleVariables)
                        .replaceAll("[\r\n]+", " ");
        String bodyHtml =
                sanitizer.sanitize(TemplateSubstitution.apply(request.content(), sampleVariables));

        Context context = new Context();
        context.setVariable("subject", subject);
        context.setVariable("bodyHtml", bodyHtml);
        String renderedHtml = templateEngine.process("email/notification-shell", context);

        return new NotificationTemplatePreviewResponse(subject, renderedHtml);
    }

    private Map<String, String> sampleVariables() {
        Map<String, String> variables = new HashMap<>();
        variables.put("customerName", "Maria Exemplo");
        variables.put("orderId", "1234");
        variables.put("orderStatus", "Aceito");
        variables.put("pickupDate", "15/09/2026");
        variables.put("pickupTime", "15:00");
        variables.put("orderTotal", "R$ 75,00");
        variables.put(
                "orderItems",
                "<ul><li><strong>Chocolate — 2 kg</strong><br>Massa: Preta &middot; Raspa: Branca<br>"
                        + "Mensagem no bolo: \"Parabéns!\"<br>Valor: R$ 75,00</li></ul>");
        variables.put("discountNotice", "Desconto aplicado: R$ 10,00.");
        variables.put(
                "orderNotesNotice",
                "<p><strong>Observações do pedido:</strong> Sem glúten, por favor</p>");
        variables.put("refundNotice", "Valor a devolver: R$ 30,00.");
        variables.put("paymentStatus", "Parcial");
        variables.put("amountPaid", "R$ 30,00");
        return variables;
    }

    private NotificationType findOrThrow(Long id) {
        return notificationTypeRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template não encontrado."));
    }

    private NotificationTemplateResponse toResponse(NotificationType type) {
        return new NotificationTemplateResponse(
                type.getId(),
                type.getCode(),
                type.getDescription(),
                type.getSubject(),
                type.getTemplateContent());
    }
}
