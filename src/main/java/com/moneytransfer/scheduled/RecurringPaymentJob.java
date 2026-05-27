package com.moneytransfer.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringPaymentJob {
    private final ScheduledPaymentService scheduledPaymentService;

    public RecurringPaymentJob(ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    @Scheduled(cron = "0 0 8 * * *")
    public void processRecurringPayments() {
        scheduledPaymentService.processDuePayments();
    }
}
