package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JavaCVFaceEngineTest {

    @Mock private FaceDetector faceDetector;
    @Mock private FaceRecognizer faceRecognizer;
    @Mock private ImagePreprocessor preprocessor;

    @Test
    void testRegisterFaceRejectsNullImage() {
        JavaCVFaceEngine engine = new JavaCVFaceEngine(faceDetector, faceRecognizer, preprocessor);
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> engine.registerFace(1L, null));
        assertTrue(e.getMessage().contains("imageBytes"));
    }

    @Test
    void testVerifyFaceRejectsNullImage() {
        JavaCVFaceEngine engine = new JavaCVFaceEngine(faceDetector, faceRecognizer, preprocessor);
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> engine.verifyFace(1L, null));
        assertTrue(e.getMessage().contains("imageBytes"));
    }

    @Test
    void testVerifyFaceReturnsFalseWhenNotTrained() {
        when(faceRecognizer.isTrained()).thenReturn(false);
        JavaCVFaceEngine engine = new JavaCVFaceEngine(faceDetector, faceRecognizer, preprocessor);
        assertFalse(engine.verifyFace(1L, new byte[]{1, 2, 3}));
    }

    @Test
    void testIsReadyDelegatesToRecognizer() {
        when(faceRecognizer.isTrained()).thenReturn(true);
        JavaCVFaceEngine engine = new JavaCVFaceEngine(faceDetector, faceRecognizer, preprocessor);
        assertTrue(engine.isReady());
    }
}
