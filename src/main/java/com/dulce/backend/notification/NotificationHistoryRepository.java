package com.dulce.backend.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationHistoryRepository extends JpaRepository<NotificationHistory, Long> {

    boolean existsByNotificationTypeIdAndCakeOrderId(Long notificationTypeId, Long cakeOrderId);
}
