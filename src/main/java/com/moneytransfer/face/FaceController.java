package com.moneytransfer.face;

import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/face")
public class FaceController {
    private final FaceService faceService;
    private final JwtUtil jwtUtil;

    public FaceController(FaceService faceService, JwtUtil jwtUtil) {
        this.faceService = faceService;
        this.jwtUtil = jwtUtil;
    }

    private Long extractUserId(Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof Claims claims)) {
            throw new IllegalArgumentException("Not authenticated");
        }
        return ((Integer) claims.get("userId")).longValue();
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registerFace(
            Authentication auth, @RequestBody Map<String, String> body) {
        try {
            Long userId = extractUserId(auth);
            String faceImage = body.get("faceImage");
            if (faceImage == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "faceImage is required"));
            }
            String msg = faceService.registerFaceSync(userId, faceImage);
            return ResponseEntity.ok(Map.of("message", msg));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, Object>> verifyFace(
            Authentication auth, @RequestBody Map<String, String> body) {
        try {
            Long userId = extractUserId(auth);
            String faceImage = body.get("faceImage");
            if (faceImage == null) {
                return ResponseEntity.badRequest().body(Map.of("error", (Object) "faceImage is required"));
            }
            boolean matched = faceService.verifyFaceBase64Sync(userId, faceImage);
            return ResponseEntity.ok(Map.of("matched", (Object) matched));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", (Object) e.getMessage()));
        }
    }

    @PostMapping("/verify-for-transfer")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> verifyForTransfer(
            Authentication auth, @RequestBody Map<String, String> body) {
        Long userId;
        try {
            userId = extractUserId(auth);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", (Object) e.getMessage())));
        }
        String faceImage = body.get("faceImage");
        if (faceImage == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", (Object) "faceImage is required")));
        }
        return faceService.verifyFaceBase64(userId, faceImage)
                .thenApply(matched -> {
                    if (matched) {
                        String faceToken = jwtUtil.generateFaceToken(userId, "TRANSFER");
                        return ResponseEntity.ok(Map.of("faceToken", (Object) faceToken));
                    } else {
                        return ResponseEntity.badRequest().body(Map.of("error", (Object) "Face verification failed"));
                    }
                })
                .exceptionally(e -> ResponseEntity.badRequest()
                        .body(Map.of("error", (Object) e.getCause().getMessage())));
    }
}
