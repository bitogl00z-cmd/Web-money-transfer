package com.moneytransfer.face;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.IMREAD_COLOR;

public class JavaCVFaceEngine {
    private static final Logger log = LoggerFactory.getLogger(JavaCVFaceEngine.class);
    private final FaceDetector faceDetector;
    private final FaceRecognizer faceRecognizer;
    private final ImagePreprocessor preprocessor;
    private final Map<Long, byte[]> faceStore = new HashMap<>();

    public JavaCVFaceEngine(FaceDetector faceDetector, FaceRecognizer faceRecognizer, ImagePreprocessor preprocessor) {
        this.faceDetector = faceDetector;
        this.faceRecognizer = faceRecognizer;
        this.preprocessor = preprocessor;
    }

    public void registerFace(Long userId, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes must not be null or empty");
        }
        Mat source = decodeImage(imageBytes);
        Mat faceRoi = faceDetector.cropLargestFace(source);
        Mat processed = preprocessor.preprocess(faceRoi);
        faceStore.put(userId, imageBytes);
        retrainModel();
        log.info("Registered face for userId: {}", userId);
    }

    public boolean verifyFace(Long userId, byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("imageBytes must not be null or empty");
        }
        if (!faceRecognizer.isTrained()) {
            log.warn("LBPH model not trained yet");
            return false;
        }
        Mat source = decodeImage(imageBytes);
        Mat faceRoi = faceDetector.cropLargestFace(source);
        Mat processed = preprocessor.preprocess(faceRoi);
        FaceRecognizer.RecognizerResult result = faceRecognizer.predict(processed);
        boolean match = result.isMatch(userId);
        log.info("Face verify for userId={}: predictedLabel={}, confidence={}, match={}",
                userId, result.label(), result.confidence(), match);
        return match;
    }

    private Mat decodeImage(byte[] imageBytes) {
        Mat raw = new Mat(new BytePointer(imageBytes));
        return imdecode(raw, IMREAD_COLOR);
    }

    private void retrainModel() {
        if (faceStore.isEmpty()) return;
        MatVector images = new MatVector(faceStore.size());
        Mat labelsMat = new Mat((int) faceStore.size(), 1, org.bytedeco.opencv.global.opencv_core.CV_32SC1);
        int i = 0;
        for (Map.Entry<Long, byte[]> entry : faceStore.entrySet()) {
            Mat source = decodeImage(entry.getValue());
            Mat faceRoi = faceDetector.cropLargestFace(source);
            images.put(i, preprocessor.preprocess(faceRoi));
            labelsMat.ptr(i).put((byte) entry.getKey().intValue());
            i++;
        }
        faceRecognizer.train(images, labelsMat);
    }

    public void addExistingFace(Long userId, byte[] imageBytes) {
        faceStore.put(userId, imageBytes);
    }

    public boolean isReady() {
        return faceRecognizer.isTrained();
    }
}
