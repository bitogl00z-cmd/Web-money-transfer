package com.moneytransfer.otp;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
public class OtpController {
    private final OtpService otpService;
    private final UserRepository userRepository;

    public OtpController(OtpService otpService, UserRepository userRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(Authentication auth, @RequestBody Map<String, String> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        User user = userRepository.findById(userId).orElseThrow();
        OtpType type = OtpType.valueOf(body.getOrDefault("type", "LOGIN"));
        otpService.generateAndSendOtp(userId, user.getEmail(), type);
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + user.getEmail()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(Authentication auth, @RequestBody Map<String, String> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        String code = body.get("code");
        OtpType type = OtpType.valueOf(body.getOrDefault("type", "LOGIN"));
        boolean valid = otpService.verifyOtp(userId, code, type);
        if (valid) {
            return ResponseEntity.ok(Map.of("message", "OTP verified"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP"));
    }
}
