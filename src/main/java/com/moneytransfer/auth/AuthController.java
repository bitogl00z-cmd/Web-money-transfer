package com.moneytransfer.auth;

import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
import com.moneytransfer.face.FaceLoginRequest;
import com.moneytransfer.face.FaceService;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final FaceService faceService;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthService authService, JwtUtil jwtUtil,
                          FaceService faceService, UserRepository userRepository,
                          AuditLogRepository auditLogRepository,
                          PasswordEncoder passwordEncoder) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
        this.faceService = faceService;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request, HttpServletResponse response) {
        try {
            AuthResponse authResponse = authService.register(request);
            authService.setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken());
            return ResponseEntity.ok(Map.of("message", "Registration successful", "userId", authResponse.userId()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request, HttpServletRequest httpRequest,
                                    HttpServletResponse response) {
        try {
            String ip = httpRequest.getRemoteAddr();
            AuthResponse authResponse = authService.login(request, ip);
            authService.setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken());
            return ResponseEntity.ok(Map.of("message", "Login successful", "userId", authResponse.userId()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractTokenFromCookies(request);
        if (token != null) {
            long ttl = jwtUtil.getExpirationMillis(token);
            authService.logout(token, ttl);
        }
        authService.clearAuthCookies(response);
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractRefreshTokenFromCookies(request);
        if (refreshToken == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "No refresh token"));
        }
        try {
            AuthResponse authResponse = authService.refreshToken(refreshToken);
            authService.setAuthCookies(response, authResponse.accessToken(), authResponse.refreshToken());
            return ResponseEntity.ok(Map.of("message", "Token refreshed"));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid refresh token"));
        }
    }

    @PostMapping("/face-login")
    public ResponseEntity<?> faceLogin(@RequestBody FaceLoginRequest request,
                                        HttpServletRequest httpRequest,
                                        HttpServletResponse response) {
        try {
            String ip = httpRequest.getRemoteAddr();
            User user = userRepository.findByUsername(request.username())
                    .orElseThrow(() -> new IllegalArgumentException("Face login failed"));
            if (user.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Account is locked"));
            }
            if (!user.isFaceEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Face login failed"));
            }
            boolean matched = faceService.verifyFaceBase64Sync(user.getId(), request.faceImage());
            if (!matched) {
                user.setFailedAttempts(user.getFailedAttempts() + 1);
                if (user.getFailedAttempts() >= 5) {
                    user.setLocked(true);
                }
                userRepository.save(user);
                return ResponseEntity.badRequest().body(Map.of("error", "Face login failed"));
            }
            user.setFailedAttempts(0);
            userRepository.save(user);

            auditLogRepository.save(new AuditLog(user.getId(), "FACE_LOGIN", "Face login successful", ip));

            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());
            authService.setAuthCookies(response, accessToken, refreshToken);

            return ResponseEntity.ok(Map.of("message", "Face login successful", "userId", user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Face login failed"));
        }
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> body,
                                             HttpServletResponse response) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long userId = ((Integer) claims.get("userId")).longValue();
            String oldPassword = body.get("oldPassword");
            String newPassword = body.get("newPassword");
            if (oldPassword == null || newPassword == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Old password and new password are required"));
            }
            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu mới phải có ít nhất 6 ký tự"));
            }
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu cũ không đúng"));
            }
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/face-quick-login")
    public ResponseEntity<?> quickFaceLogin(@RequestBody Map<String, String> body,
                                             HttpServletRequest httpRequest,
                                             HttpServletResponse response) {
        try {
            String faceImage = body.get("faceImage");
            if (faceImage == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "faceImage is required"));
            }
            User user = faceService.identifyFaceBase64Sync(faceImage);
            if (user.isLocked()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Tài khoản đã bị khóa"));
            }
            if (!user.isFaceEnabled()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Chưa đăng ký gương mặt"));
            }
            user.setFailedAttempts(0);
            userRepository.save(user);

            String ip = httpRequest.getRemoteAddr();
            auditLogRepository.save(new AuditLog(user.getId(), "FACE_LOGIN", "Quick face login successful", ip));

            String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
            String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());
            authService.setAuthCookies(response, accessToken, refreshToken);

            return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công", "userId", user.getId()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    private String extractTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private String extractRefreshTokenFromCookies(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
