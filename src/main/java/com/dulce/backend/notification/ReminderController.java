package com.dulce.backend.notification;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/api/notifications/reminders")
public class ReminderController {

    private final PickupReminderScheduler pickupReminderScheduler;

    public ReminderController(PickupReminderScheduler pickupReminderScheduler) {
        this.pickupReminderScheduler = pickupReminderScheduler;
    }

    @PostMapping("/run")
    public ReminderRunResult run() {
        return pickupReminderScheduler.sendReminders();
    }
}
