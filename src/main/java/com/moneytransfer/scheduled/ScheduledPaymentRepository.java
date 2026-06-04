package com.moneytransfer.scheduled;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {
    List<ScheduledPayment> findByFromAccountId(Long fromAccountId);
    List<ScheduledPayment> findByStatusAndNextRunLessThanEqual(PaymentStatus status, LocalDateTime dateTime);
}
