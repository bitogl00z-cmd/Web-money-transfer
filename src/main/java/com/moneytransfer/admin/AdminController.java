package com.moneytransfer.admin;

import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.user.UserDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @GetMapping("/audit-logs")
    public Page<AuditLog> getAuditLogs(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return adminService.getAuditLogs(PageRequest.of(page, size));
    }
}
