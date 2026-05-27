package com.moneytransfer.scheduled;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled-payments")
public class ScheduledPaymentController {
    private final ScheduledPaymentService scheduledPaymentService;

    public ScheduledPaymentController(ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    @GetMapping
    public ResponseEntity<?> getUserPayments(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return ResponseEntity.ok(scheduledPaymentService.getUserPayments(userId));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        ScheduledPayment payment = new ScheduledPayment();
        payment.setFromAccountId(Long.valueOf(body.get("fromAccountId").toString()));
        payment.setToAccountNumber(body.get("toAccountNumber").toString());
        payment.setAmount(new BigDecimal(body.get("amount").toString()));
        payment.setDescription((String) body.getOrDefault("description", ""));
        payment.setFrequency(PaymentFrequency.valueOf(body.get("frequency").toString()));
        payment.setNextRun(LocalDate.parse(body.get("nextRun").toString()));
        return ResponseEntity.ok(scheduledPaymentService.create(payment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        scheduledPaymentService.cancel(id);
        return ResponseEntity.ok(Map.of("message", "Payment cancelled"));
    }
}
