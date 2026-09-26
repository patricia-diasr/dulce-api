package com.dulce.backend.notification;

import java.util.Map;

public interface NotificationService {

    void send(String to, String subject, String templateName, Map<String, Object> variables);

    void notifyOrderCreated(Long orderId);

    void notifyOrderUpdated(Long orderId);

    void notifyOrderAccepted(Long orderId);

    void notifyOrderRejected(Long orderId);

    void notifyOrderCanceled(Long orderId);

    void notifyOrderCompleted(Long orderId);

    void notifyPaymentRegistered(Long orderId, Long paymentId);
}
