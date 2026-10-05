package com.dulce.backend.notification;

import com.dulce.backend.order.CakeOrder;
import com.dulce.backend.order.CakeOrderRepository;
import com.dulce.backend.order.CakeOrderSpecifications;
import com.dulce.backend.order.OrderStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PickupReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(PickupReminderScheduler.class);
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");

    private final CakeOrderRepository cakeOrderRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final NotificationHistoryRepository notificationHistoryRepository;
    private final NotificationService notificationService;

    public PickupReminderScheduler(
            CakeOrderRepository cakeOrderRepository,
            NotificationTypeRepository notificationTypeRepository,
            NotificationHistoryRepository notificationHistoryRepository,
            NotificationService notificationService) {
        this.cakeOrderRepository = cakeOrderRepository;
        this.notificationTypeRepository = notificationTypeRepository;
        this.notificationHistoryRepository = notificationHistoryRepository;
        this.notificationService = notificationService;
    }

    @Scheduled(cron = "0 0 8 * * *", zone = "America/Sao_Paulo")
    @Transactional(readOnly = true)
    public ReminderRunResult sendReminders() {
        NotificationType reminderType =
                notificationTypeRepository
                        .findByCode(NotificationTypeCode.PICKUP_REMINDER.name())
                        .orElse(null);
        if (reminderType == null) {
            log.warn("Template PICKUP_REMINDER não encontrado — lembrete não enviado hoje.");
            return new ReminderRunResult(0, 0, 0);
        }

        LocalDate tomorrow = LocalDate.now(BUSINESS_ZONE).plusDays(1);
        OffsetDateTime from = tomorrow.atStartOfDay(BUSINESS_ZONE).toOffsetDateTime();
        OffsetDateTime to = tomorrow.atTime(LocalTime.MAX).atZone(BUSINESS_ZONE).toOffsetDateTime();

        Specification<CakeOrder> spec =
                Specification.where(CakeOrderSpecifications.pickupBetween(from, to))
                        .and(CakeOrderSpecifications.statusEquals(OrderStatus.ACCEPTED));

        List<CakeOrder> candidates = cakeOrderRepository.findAll(spec);
        log.info(
                "Lembrete de retirada: {} pedido(s) candidato(s) para {}.",
                candidates.size(),
                tomorrow);

        int sentCount = 0;
        int skippedCount = 0;

        for (CakeOrder order : candidates) {
            boolean alreadySent =
                    notificationHistoryRepository.existsByNotificationTypeIdAndCakeOrderId(
                            reminderType.getId(), order.getId());
            if (alreadySent) {
                skippedCount++;
                continue;
            }
            try {
                notificationService.notifyPickupReminder(order.getId());
                sentCount++;
            } catch (Exception e) {
                log.error("Falha ao enviar lembrete do pedido {}.", order.getId(), e);
            }
        }

        return new ReminderRunResult(candidates.size(), sentCount, skippedCount);
    }
}
