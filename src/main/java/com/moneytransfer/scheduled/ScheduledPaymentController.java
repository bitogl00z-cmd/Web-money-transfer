package com.moneytransfer.scheduled;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountService;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
        List<Account> accounts = accountService.getUserAccounts(userId);
        if (accounts.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "No accounts"));
        }
        Long fromAccountId = accounts.get(0).getId();

        ScheduledPayment payment = new ScheduledPayment();
        payment.setFromAccountId(fromAccountId);
        payment.setToAccountNumber(body.get("toAccountNumber").toString());
        payment.setAmount(new BigDecimal(body.get("amount").toString()));
        payment.setDescription((String) body.getOrDefault("description", ""));

        LocalDate date = LocalDate.parse(body.get("scheduledDate").toString());
        int hour = Integer.parseInt(body.get("scheduledHour").toString());
        int minute = Integer.parseInt(body.get("scheduledMinute").toString());
        LocalDateTime scheduledTime = LocalDateTime.of(date, LocalTime.of(hour, minute));
        payment.setStartDate(scheduledTime);
        payment.setNextRun(scheduledTime);

        if (body.containsKey("endDate") && body.get("endDate") != null && !body.get("endDate").toString().isEmpty()) {
            LocalDate endDate = LocalDate.parse(body.get("endDate").toString());
            int endHour = body.containsKey("endHour") ? Integer.parseInt(body.get("endHour").toString()) : 23;
            int endMinute = body.containsKey("endMinute") ? Integer.parseInt(body.get("endMinute").toString()) : 59;
            payment.setEndDate(LocalDateTime.of(endDate, LocalTime.of(endHour, endMinute)));
        }

        if (body.containsKey("frequency")) {
            payment.setFrequency(PaymentFrequency.valueOf(body.get("frequency").toString()));
        } else {
            payment.setFrequency(PaymentFrequency.ONE_TIME);
        }
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

    @GetMapping("/accounts")
    public ResponseEntity<?> getAccounts(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        List<Account> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }
}
