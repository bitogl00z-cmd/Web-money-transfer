package com.moneytransfer.transaction;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final JwtUtil jwtUtil;

    public TransactionController(TransactionService transactionService, AccountService accountService,
                                  AccountRepository accountRepository, JwtUtil jwtUtil) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody Map<String, Object> body,
                                       Authentication auth, HttpServletRequest request) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long userId = ((Integer) claims.get("userId")).longValue();
            Long fromId = Long.valueOf(body.get("fromAccountId").toString());
            String toAccountNumber = body.get("toAccountNumber").toString();
            Account toAccount = accountRepository.findByAccountNumber(toAccountNumber)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found: " + toAccountNumber));
            Long toId = toAccount.getId();
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String description = (String) body.getOrDefault("description", "");

            // Face verification check for amounts > 10,000,000
            BigDecimal threshold = new BigDecimal("10000000");
            if (amount.compareTo(threshold) > 0) {
                String faceToken = (String) body.get("faceToken");
                if (faceToken == null || faceToken.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of(
                        "error", "Face verification required for amounts over 10,000,000₫",
                        "faceRequired", true
                    ));
                }
                try {
                    Claims faceClaims = jwtUtil.validateToken(faceToken);
                    Long faceUserId = ((Integer) faceClaims.get("userId")).longValue();
                    String purpose = faceClaims.get("purpose", String.class);
                    if (!faceUserId.equals(userId) || !"TRANSFER".equals(purpose)) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Invalid face verification token"));
                    }
                } catch (Exception e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Face verification token expired or invalid"));
                }
            }

            String ip = request.getRemoteAddr();
            Transaction tx = transactionService.transfer(fromId, toId, amount, description, userId, ip);
            return ResponseEntity.ok(Map.of("transactionCode", tx.getTransactionCode(), "status", tx.getStatus()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> body,
                                       Authentication auth, HttpServletRequest request) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long userId = ((Integer) claims.get("userId")).longValue();
            Long accountId = Long.valueOf(body.get("accountId").toString());
            Account account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            if (!account.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String ip = request.getRemoteAddr();
            Transaction tx = transactionService.deposit(accountId, amount, "Deposit", userId, ip);
            return ResponseEntity.ok(Map.of("transactionCode", tx.getTransactionCode(), "status", tx.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> body,
                                       Authentication auth, HttpServletRequest request) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long userId = ((Integer) claims.get("userId")).longValue();
            Long accountId = Long.valueOf(body.get("accountId").toString());
            Account account = accountService.findById(accountId)
                    .orElseThrow(() -> new IllegalArgumentException("Account not found"));
            if (!account.getUserId().equals(userId)) {
                return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
            }
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String ip = request.getRemoteAddr();
            Transaction tx = transactionService.withdraw(accountId, amount, "Withdrawal", userId, ip);
            return ResponseEntity.ok(Map.of("transactionCode", tx.getTransactionCode(), "status", tx.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication auth,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        List<Account> accounts = accountService.getUserAccounts(userId);
        List<Long> accountIds = accounts.stream().map(Account::getId).collect(Collectors.toList());
        Page<Transaction> history = transactionService.getHistory(accountIds, PageRequest.of(page, size));
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(tx -> ResponseEntity.ok(Map.of(
                        "transactionCode", tx.getTransactionCode(),
                        "amount", tx.getAmount(),
                        "type", tx.getType(),
                        "status", tx.getStatus(),
                        "createdAt", tx.getCreatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
