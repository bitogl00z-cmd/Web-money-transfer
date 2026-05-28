package com.moneytransfer.face;

import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<Map<String, String>>> registerFace(
            Authentication auth, @RequestParam("file") MultipartFile file) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return faceService.registerFace(userId, file)
                .thenApply(msg -> ResponseEntity.ok(Map.of("message", msg)))
                .exceptionally(e -> ResponseEntity.badRequest()
                        .body(Map.of("error", e.getCause().getMessage())));
    }

    @PostMapping("/verify")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> verifyFace(
            Authentication auth, @RequestParam("file") MultipartFile file) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return faceService.verifyFace(userId, file)
                .thenApply(match -> ResponseEntity.ok(Map.of("matched", (Object) match)))
                .exceptionally(e -> ResponseEntity.badRequest()
                        .body(Map.of("error", (Object) e.getCause().getMessage())));
    }

    @PostMapping("/verify-for-transfer")
    public CompletableFuture<ResponseEntity<Map<String, Object>>> verifyForTransfer(
            Authentication auth, @RequestBody Map<String, String> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        String faceImage = body.get("faceImage");
        if (faceImage == null) {
            return CompletableFuture.completedFuture(
                    ResponseEntity.badRequest().body(Map.of("error", "faceImage is required")));
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
