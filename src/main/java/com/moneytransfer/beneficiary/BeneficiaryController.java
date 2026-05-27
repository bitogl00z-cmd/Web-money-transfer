package com.moneytransfer.beneficiary;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public ResponseEntity<?> getBeneficiaries(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return ResponseEntity.ok(beneficiaryService.getUserBeneficiaries(userId));
    }

    @PostMapping
    public ResponseEntity<?> addBeneficiary(Authentication auth, @RequestBody Map<String, String> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        try {
            Beneficiary beneficiary = beneficiaryService.addBeneficiary(userId,
                    body.get("accountNumber"), body.get("nickname"));
            return ResponseEntity.ok(Map.of("id", beneficiary.getId(), "message", "Beneficiary added"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBeneficiary(Authentication auth, @PathVariable Long id) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        try {
            beneficiaryService.deleteBeneficiary(id, userId);
            return ResponseEntity.ok(Map.of("message", "Beneficiary deleted"));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
