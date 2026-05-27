package com.moneytransfer.face;

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

    public FaceController(FaceService faceService) {
        this.faceService = faceService;
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
}
