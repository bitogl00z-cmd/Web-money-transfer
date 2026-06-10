package com.moneytransfer.user;

import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
                .map(user -> ResponseEntity.ok(Map.ofEntries(
                        Map.entry("id", user.getId()),
                        Map.entry("username", user.getUsername()),
                        Map.entry("email", user.getEmail()),
                        Map.entry("fullName", user.getFullName()),
                        Map.entry("phone", user.getPhone() != null ? user.getPhone() : ""),
                        Map.entry("tier", user.getTier().name()),
                        Map.entry("otpEnabled", user.isOtpEnabled()),
                        Map.entry("faceEnabled", user.isFaceEnabled()),
                        Map.entry("avatarUrl", user.getAvatarUrl() != null ? user.getAvatarUrl() : ""),
                        Map.entry("faceImageUrl", user.getFaceImageUrl() != null ? user.getFaceImageUrl() : ""),
                        Map.entry("pinSet", user.getPinSet() != null && user.getPinSet()),
                        Map.entry("emailNotifications", user.isEmailNotifications()),
                        Map.entry("language", user.getLanguage() != null ? user.getLanguage() : "vi")
                )))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(Authentication auth, @RequestParam(value = "avatar", required = false) MultipartFile avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Avatar file is required"));
        }
        try {
            Claims claims = (Claims) auth.getDetails();
            Long userId = ((Integer) claims.get("userId")).longValue();
            String avatarUrl = userService.updateAvatar(userId, avatar);
            return ResponseEntity.ok(Map.of("avatarUrl", avatarUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload avatar"));
        }
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
