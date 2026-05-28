package com.moneytransfer.face;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class FaceService {
    private final FaceRecognitionUtil faceUtil;
    private final UserRepository userRepository;
    private final Executor faceExecutor;
    private final double similarityThreshold;

    public FaceService(FaceRecognitionUtil faceUtil, UserRepository userRepository,
                       @Qualifier("faceExecutor") Executor faceExecutor,
                       @Value("${app.face.similarity-threshold:0.7}") double similarityThreshold) {
        this.faceUtil = faceUtil;
        this.userRepository = userRepository;
        this.faceExecutor = faceExecutor;
        this.similarityThreshold = similarityThreshold;
    }

    public CompletableFuture<String> registerFace(Long userId, MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            try {
                MatOfByte matOfByte = new MatOfByte(file.getBytes());
                Mat image = faceUtil.resizeToMax(Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR));
                Rect faceRect = faceUtil.detectFace(image);
                double[] encoding = faceUtil.computeEncoding(image, faceRect);
                String encoded = Arrays.stream(encoding)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(","));
                user.setFaceEncoding(encoded);
                user.setFaceEnabled(true);
                userRepository.save(user);
                return "Face registered successfully";
            } catch (Exception e) {
                throw new RuntimeException("Face registration failed: " + e.getMessage());
            }
        }, faceExecutor);
    }

    public CompletableFuture<Boolean> verifyFace(Long userId, MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (user.getFaceEncoding() == null) {
                throw new IllegalArgumentException("No face registered");
            }
            try {
                MatOfByte matOfByte = new MatOfByte(file.getBytes());
                Mat image = faceUtil.resizeToMax(Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR));
                Rect faceRect = faceUtil.detectFace(image);
                double[] encoding = faceUtil.computeEncoding(image, faceRect);
                double[] stored = Arrays.stream(user.getFaceEncoding().split(","))
                        .mapToDouble(Double::parseDouble).toArray();
                double similarity = faceUtil.cosineSimilarity(encoding, stored);
                return similarity >= similarityThreshold;
            } catch (Exception e) {
                throw new RuntimeException("Face verification failed: " + e.getMessage());
            }
        }, faceExecutor);
    }

    public CompletableFuture<Boolean> verifyFaceBase64(Long userId, String base64Image) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (user.getFaceEncoding() == null) {
                throw new IllegalArgumentException("No face registered");
            }
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                MatOfByte matOfByte = new MatOfByte(imageBytes);
                Mat image = faceUtil.resizeToMax(Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR));
                Rect faceRect = faceUtil.detectFace(image);
                double[] encoding = faceUtil.computeEncoding(image, faceRect);
                double[] stored = Arrays.stream(user.getFaceEncoding().split(","))
                        .mapToDouble(Double::parseDouble).toArray();
                double similarity = faceUtil.cosineSimilarity(encoding, stored);
                return similarity >= similarityThreshold;
            } catch (Exception e) {
                throw new RuntimeException("Face verification failed: " + e.getMessage());
            }
        }, faceExecutor);
    }
}
