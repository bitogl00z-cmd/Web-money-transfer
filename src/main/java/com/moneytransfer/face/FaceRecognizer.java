package com.moneytransfer.face;

import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.javacpp.DoublePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_face.LBPHFaceRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class FaceRecognizer {
    private static final Logger log = LoggerFactory.getLogger(FaceRecognizer.class);
    private static final double CONFIDENCE_THRESHOLD = 80.0;
    private final String modelPath;
    private final LBPHFaceRecognizer recognizer;

    public FaceRecognizer(String modelPath) {
        if (modelPath == null || modelPath.isBlank()) {
            throw new IllegalArgumentException("modelPath must not be null or empty");
        }
        this.modelPath = modelPath;
        this.recognizer = LBPHFaceRecognizer.create();
        if (Files.exists(Path.of(modelPath))) {
            recognizer.read(modelPath);
            log.info("Loaded LBPH model from: {}", modelPath);
        } else {
            log.info("No existing model at {}, will train on first registration", modelPath);
        }
    }

    public void train(MatVector images, Mat labels) {
        recognizer.train(images, labels);
        recognizer.save(modelPath);
        log.info("Trained and saved LBPH model to: {}", modelPath);
    }

    public RecognizerResult predict(Mat faceRoi) {
        IntPointer label = new IntPointer(1);
        DoublePointer confidence = new DoublePointer(1);
        recognizer.predict(faceRoi, label, confidence);
        return new RecognizerResult(label.get(0), confidence.get(0));
    }

    public boolean isTrained() {
        return !recognizer.empty();
    }

    public record RecognizerResult(int label, double confidence) {
        public boolean isMatch(long expectedUserId) {
            return label == expectedUserId && confidence < CONFIDENCE_THRESHOLD;
        }
    }
}
