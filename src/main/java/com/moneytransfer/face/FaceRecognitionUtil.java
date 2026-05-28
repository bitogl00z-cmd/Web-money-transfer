package com.moneytransfer.face;

import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

@Component
public class FaceRecognitionUtil {
    private final CascadeClassifier faceDetector;
    private final int maxImageSize;

    static { nu.pattern.OpenCV.loadLocally(); }

    public FaceRecognitionUtil(@Value("${app.face.max-image-size:400}") int maxImageSize) {
        this.maxImageSize = maxImageSize;
        try {
            File cascadeFile = new ClassPathResource("haarcascade_frontalface_default.xml").getFile();
            this.faceDetector = new CascadeClassifier(cascadeFile.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to load haarcascade_frontalface_default.xml from classpath", e);
        }
    }

    public Mat resizeToMax(Mat src) {
        int maxDim = Math.max(src.width(), src.height());
        if (maxDim <= maxImageSize) return src;
        double scale = (double) maxImageSize / maxDim;
        Mat resized = new Mat();
        Imgproc.resize(src, resized, new Size(), scale, scale);
        return resized;
    }

    public Rect detectFace(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces);
        Rect[] rects = faces.toArray();
        if (rects.length == 0) throw new RuntimeException("No face detected");
        return rects[0];
    }

    public double[] computeEncoding(Mat image, Rect faceRect) {
        Mat face = new Mat(image, faceRect);
        Mat gray = new Mat();
        Imgproc.cvtColor(face, gray, Imgproc.COLOR_BGR2GRAY);
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(128, 128));
        double[] encoding = new double[128 * 128];
        resized.get(0, 0, encoding);
        return normalize(encoding);
    }

    public double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double[] normalize(double[] vec) {
        double norm = 0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm == 0) return vec;
        for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        return vec;
    }
}
