package com.moneytransfer.recent;

import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recent-transfers")
public class RecentTransferController {
    private final RecentTransferService recentTransferService;

    public RecentTransferController(RecentTransferService recentTransferService) {
        this.recentTransferService = recentTransferService;
    }

    @GetMapping
    public ResponseEntity<?> getRecentTransfers(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        List<RecentTransfer> list = recentTransferService.getRecentTransfers(userId);
        return ResponseEntity.ok(list);
    }
}
