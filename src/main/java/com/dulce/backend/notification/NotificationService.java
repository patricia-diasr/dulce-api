package com.dulce.backend.notification;

import java.util.Map;

public interface NotificationService {

    void send(String to, String subject, String templateName, Map<String, Object> variables);
}
