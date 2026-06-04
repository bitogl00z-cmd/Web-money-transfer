package com.moneytransfer.config;

import com.moneytransfer.face.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class FaceProcessingConfig {
    private static final Logger log = LoggerFactory.getLogger(FaceProcessingConfig.class);

    @Value("${app.face.thread-pool-size:4}")
    private int poolSize;

    @Value("${app.face.model-dir:${user.home}/.money-transfer}")
    private String modelDir;

    @Bean
    public LBPHModelManager lbphModelManager() {
        return new LBPHModelManager(modelDir);
    }

    @Bean
    public ImagePreprocessor imagePreprocessor() {
        return new ImagePreprocessor();
    }

    @Bean
    public FaceDetector faceDetector(ImagePreprocessor preprocessor, LBPHModelManager modelManager) {
        String cascadePath = modelManager.getHaarCascadePath();
        Path cascadeFile = Path.of(cascadePath);
        if (!Files.exists(cascadeFile)) {
            try {
                Files.createDirectories(cascadeFile.getParent());
                ClassPathResource resource = new ClassPathResource("haarcascade_frontalface_default.xml");
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, cascadeFile, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Extracted Haar cascade to: {}", cascadePath);
                    }
                } else {
                    log.warn("Haar cascade not found in classpath, bundled JAR may not include it");
                }
            } catch (Exception e) {
                log.warn("Could not extract Haar cascade: {}", e.getMessage());
            }
        }
        return new FaceDetector(cascadePath, preprocessor);
    }

    @Bean
    public FaceRecognizer faceRecognizer(LBPHModelManager modelManager) {
        return new FaceRecognizer(modelManager.getModelFilePath());
    }

    @Bean
    public JavaCVFaceEngine javaCVFaceEngine(FaceDetector faceDetector, FaceRecognizer faceRecognizer,
                                              ImagePreprocessor preprocessor) {
        return new JavaCVFaceEngine(faceDetector, faceRecognizer, preprocessor);
    }

    @Bean("faceExecutor")
    public Executor faceExecutor() {
        return Executors.newFixedThreadPool(poolSize);
    }
}
