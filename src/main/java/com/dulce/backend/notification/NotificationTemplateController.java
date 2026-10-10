package com.dulce.backend.notification;

import com.dulce.backend.notification.dto.NotificationTemplatePreviewRequest;
import com.dulce.backend.notification.dto.NotificationTemplatePreviewResponse;
import com.dulce.backend.notification.dto.NotificationTemplateResponse;
import com.dulce.backend.notification.dto.NotificationTemplateUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/notifications/templates")
public class NotificationTemplateController {

    private final NotificationTemplateService notificationTemplateService;

    public NotificationTemplateController(NotificationTemplateService notificationTemplateService) {
        this.notificationTemplateService = notificationTemplateService;
    }

    @GetMapping
    public List<NotificationTemplateResponse> list() {
        return notificationTemplateService.list();
    }

    @GetMapping("/{id}")
    public NotificationTemplateResponse getById(@PathVariable Long id) {
        return notificationTemplateService.getById(id);
    }

    @PatchMapping("/{id}")
    public NotificationTemplateResponse update(
            @PathVariable Long id, @Valid @RequestBody NotificationTemplateUpdateRequest request) {
        return notificationTemplateService.update(id, request);
    }

    @PostMapping("/preview")
    public NotificationTemplatePreviewResponse preview(
            @Valid @RequestBody NotificationTemplatePreviewRequest request) {
        return notificationTemplateService.preview(request);
    }
}
