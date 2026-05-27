package com.moneytransfer.account;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    public ResponseEntity<?> getUserAccounts(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        List<Account> accounts = accountService.getUserAccounts(userId);
        return ResponseEntity.ok(accounts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccount(Authentication auth, @PathVariable Long id) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return accountService.findById(id)
                .map(account -> {
                    if (!account.getUserId().equals(userId)) {
                        return ResponseEntity.status(403).body(Map.of("error", "Access denied"));
                    }
                    return ResponseEntity.ok(Map.of(
                            "id", account.getId(),
                            "accountNumber", account.getAccountNumber(),
                            "balance", account.getBalance(),
                            "status", account.getStatus()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
