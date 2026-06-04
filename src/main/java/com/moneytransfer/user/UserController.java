package com.moneytransfer.user;

import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<?> getProfile(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return userService.findById(userId)
                .map(user -> ResponseEntity.ok(Map.of(
                        "id", user.getId(),
                        "username", user.getUsername(),
                        "email", user.getEmail(),
                        "fullName", user.getFullName(),
                        "phone", user.getPhone() != null ? user.getPhone() : "",
                        "tier", user.getTier().name(),
                        "otpEnabled", user.isOtpEnabled(),
                        "faceEnabled", user.isFaceEnabled(),
                        "emailNotifications", user.isEmailNotifications(),
                        "language", user.getLanguage() != null ? user.getLanguage() : "vi"
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Authentication auth, @RequestBody Map<String, String> body) {
        try {
            String username = auth.getName();
            userService.updateProfile(username, body);
            return ResponseEntity.ok(Map.of("message", "Profile updated"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
