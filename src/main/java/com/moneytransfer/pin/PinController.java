package com.moneytransfer.pin;

import com.moneytransfer.auth.JwtUtil;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pin")
public class PinController {
    private final PinService pinService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public PinController(PinService pinService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.pinService = pinService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/set")
    public ResponseEntity<?> setPin(@RequestBody Map<String, String> body, Authentication auth) {
        String pin = body.get("pin");
        String oldPin = body.get("oldPin");
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        User user = userRepository.findById(userId).orElseThrow();
        try {
            pinService.setPin(user, pin, oldPin);
            return ResponseEntity.ok(Map.of("message", "PIN set successfully", "pinSet", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, String> body, Authentication auth) {
        String pin = body.get("pin");
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        User user = userRepository.findById(userId).orElseThrow();
        if (pinService.verifyPin(user, pin)) {
            String pinToken = jwtUtil.generateToken(userId, user.getUsername(), user.getRole().name(), 300);
            return ResponseEntity.ok(Map.of("valid", true, "pinToken", pinToken));
        }
        return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Invalid PIN"));
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removePin() {
        return ResponseEntity.badRequest().body(Map.of("error", "Cannot remove PIN. You can only change it."));
    }
}
