package com.moneytransfer.face;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class LBPHModelManager {
    private static final Logger log = LoggerFactory.getLogger(LBPHModelManager.class);
    private final Path modelDir;

    public LBPHModelManager(String modelDirPath) {
        this.modelDir = Path.of(modelDirPath);
        try {
            Files.createDirectories(this.modelDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create model directory: " + modelDirPath, e);
        }
        log.info("LBPH model directory: {}", this.modelDir);
    }

    public String getModelFilePath() {
        return modelDir.resolve("face-model.xml").toAbsolutePath().toString();
    }

    public String getHaarCascadePath() {
        return modelDir.resolve("haarcascade_frontalface_default.xml").toAbsolutePath().toString();
    }

    public Path getModelDir() {
        return modelDir;
    }
}
