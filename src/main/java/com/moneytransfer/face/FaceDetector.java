package com.moneytransfer.face;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_objdetect.CascadeClassifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FaceDetector {
    private static final Logger log = LoggerFactory.getLogger(FaceDetector.class);
    private final CascadeClassifier cascade;
    private final ImagePreprocessor preprocessor;

    public FaceDetector(String cascadePath, ImagePreprocessor preprocessor) {
        if (cascadePath == null || cascadePath.isBlank()) {
            throw new IllegalArgumentException("cascadePath must not be null or empty");
        }
        if (preprocessor == null) {
            throw new IllegalArgumentException("preprocessor must not be null");
        }
        this.preprocessor = preprocessor;
        this.cascade = new CascadeClassifier(cascadePath);
        if (this.cascade.empty()) {
            throw new IllegalStateException("Failed to load cascade classifier from: " + cascadePath);
        }
        log.info("FaceDetector initialized with cascade: {}", cascadePath);
    }

    public RectVector detect(Mat source) {
        Mat gray = preprocessor.toGrayScale(source);
        Mat equalized = preprocessor.equalizeHistogram(gray);
        RectVector faces = new RectVector();
        cascade.detectMultiScale(equalized, faces, 1.1, 3, 0, new Size(100, 100), new Size());
        return faces;
    }

    public Mat cropLargestFace(Mat source) {
        RectVector faces = detect(source);
        if (faces.empty()) {
            throw new IllegalArgumentException("No face detected in image");
        }
        Rect largest = faces.get(0);
        long maxArea = largest.width() * largest.height();
        for (int i = 1; i < faces.size(); i++) {
            Rect r = faces.get(i);
            long area = r.width() * r.height();
            if (area > maxArea) {
                largest = r;
                maxArea = area;
            }
        }
        return source.apply(largest);
    }
}
