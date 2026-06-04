package com.moneytransfer.face;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import static org.bytedeco.opencv.global.opencv_imgproc.*;

public class ImagePreprocessor {

    public static final int FACE_WIDTH = 200;
    public static final int FACE_HEIGHT = 200;

    public Mat toGrayScale(Mat source) {
        Mat gray = new Mat();
        cvtColor(source, gray, COLOR_BGR2GRAY);
        return gray;
    }

    public Mat equalizeHistogram(Mat gray) {
        Mat equalized = new Mat();
        equalizeHist(gray, equalized);
        return equalized;
    }

    public Mat resizeToStandard(Mat faceRoi) {
        Mat resized = new Mat();
        resize(faceRoi, resized, new Size(FACE_WIDTH, FACE_HEIGHT));
        return resized;
    }

    public Mat preprocess(Mat source) {
        Mat gray = toGrayScale(source);
        Mat equalized = equalizeHistogram(gray);
        return resizeToStandard(equalized);
    }
}
