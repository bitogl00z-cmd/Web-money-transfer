package com.moneytransfer.scheduled;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled-payments")
public class ScheduledPaymentController {
    private final ScheduledPaymentService scheduledPaymentService;
    private final AccountService accountService;

    public ScheduledPaymentController(ScheduledPaymentService scheduledPaymentService, AccountService accountService) {
        this.scheduledPaymentService = scheduledPaymentService;
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<?> getUserPayments(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        List<Account> accounts = accountService.getUserAccounts(userId);
        List<ScheduledPayment> payments = new ArrayList<>();
        for (Account account : accounts) {
            payments.addAll(scheduledPaymentService.getUserPayments(account.getId()));
        }
        return ResponseEntity.ok(payments);
    }

    @PostMapping
    public ResponseEntity<?> create(Authentication auth, @RequestBody Map<String, Object> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        Long fromAccountId = Long.valueOf(body.get("fromAccountId").toString());

        Account account = accountService.findById(fromAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        ScheduledPayment payment = new ScheduledPayment();
        payment.setFromAccountId(fromAccountId);
        payment.setToAccountNumber(body.get("toAccountNumber").toString());
        payment.setAmount(new BigDecimal(body.get("amount").toString()));
        payment.setDescription((String) body.getOrDefault("description", ""));
        payment.setFrequency(PaymentFrequency.valueOf(body.get("frequency").toString()));
        payment.setNextRun(LocalDate.parse(body.get("nextRun").toString()));
        return ResponseEntity.ok(scheduledPaymentService.create(payment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(Authentication auth, @PathVariable Long id) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();

        ScheduledPayment payment = scheduledPaymentService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));

        Account account = accountService.findById(payment.getFromAccountId())
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        if (!account.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
        }

        scheduledPaymentService.cancel(id);
        return ResponseEntity.ok(Map.of("message", "Payment cancelled"));
    }
}
