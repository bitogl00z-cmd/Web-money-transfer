package com.moneytransfer.face;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class FaceService {
    private final JavaCVFaceEngine javaCVFaceEngine;
    private final UserRepository userRepository;
    private final Executor faceExecutor;

    public FaceService(JavaCVFaceEngine javaCVFaceEngine, UserRepository userRepository,
                       @Qualifier("faceExecutor") Executor faceExecutor) {
        this.javaCVFaceEngine = javaCVFaceEngine;
        this.userRepository = userRepository;
        this.faceExecutor = faceExecutor;
    }

    public String registerFaceSync(Long userId, String base64Image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            javaCVFaceEngine.registerFace(userId, imageBytes);
            user.setFaceEnabled(true);
            userRepository.save(user);
            return "Face registered successfully";
        } catch (Exception e) {
            throw new RuntimeException("Face registration failed: " + e.getMessage());
        }
    }

    public CompletableFuture<Boolean> verifyFaceBase64(Long userId, String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            throw new IllegalArgumentException("Face image is required");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!user.isFaceEnabled()) {
            throw new IllegalArgumentException("No face registered");
        }
        return CompletableFuture.supplyAsync(() -> {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                return javaCVFaceEngine.verifyFace(userId, imageBytes);
            } catch (Exception e) {
                throw new RuntimeException("Face verification failed", e);
            }
        }, faceExecutor);
    }

    public boolean verifyFaceBase64Sync(Long userId, String base64Image) {
        try {
            return verifyFaceBase64(userId, base64Image).get(15, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException | InterruptedException e) {
            throw new RuntimeException("Face verification failed", e);
        }
    }
}
