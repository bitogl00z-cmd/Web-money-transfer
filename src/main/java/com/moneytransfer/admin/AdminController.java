package com.moneytransfer.admin;

import com.moneytransfer.account.Account;
import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.user.UserDto;
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

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<UserDto> getUsers(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size) {
        return adminService.getUsers(PageRequest.of(page, size));
    }

    @PostMapping("/users/{id}/lock")
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        adminService.lockUser(id);
        return ResponseEntity.ok(Map.of("message", "User locked"));
    }

    @PostMapping("/users/{id}/unlock")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        adminService.unlockUser(id);
        return ResponseEntity.ok(Map.of("message", "User unlocked"));
    }

    @GetMapping("/transactions")
    public Page<AdminTransactionDto> getTransactions(@RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        return adminService.getTransactions(PageRequest.of(page, size));
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return adminService.getStats();
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> getAuditLogs(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return adminService.getAuditLogs(PageRequest.of(page, size));
    }

    @GetMapping("/accounts")
    public List<Account> getAccounts() {
        return adminService.getAllAccounts();
    }

    @PostMapping("/accounts/{id}/deposit")
    public ResponseEntity<?> deposit(@PathVariable Long id, @RequestBody Map<String, String> body,
                                      Authentication auth, HttpServletRequest request) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long adminId = ((Integer) claims.get("userId")).longValue();
            BigDecimal amount = new BigDecimal(body.get("amount"));
            String desc = body.getOrDefault("description", "Admin deposit");
            Transaction tx = adminService.deposit(id, amount, desc, adminId, request.getRemoteAddr());
            return ResponseEntity.ok(Map.of("message", "Nạp tiền thành công", "transactionCode", tx.getTransactionCode()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/accounts/{id}/withdraw")
    public ResponseEntity<?> withdraw(@PathVariable Long id, @RequestBody Map<String, String> body,
                                       Authentication auth, HttpServletRequest request) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long adminId = ((Integer) claims.get("userId")).longValue();
            BigDecimal amount = new BigDecimal(body.get("amount"));
            String desc = body.getOrDefault("description", "Admin withdrawal");
            Transaction tx = adminService.withdraw(id, amount, desc, adminId, request.getRemoteAddr());
            return ResponseEntity.ok(Map.of("message", "Rút tiền thành công", "transactionCode", tx.getTransactionCode()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
