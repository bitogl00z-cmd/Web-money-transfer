package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FaceDetectorTest {

    @Test
    void testConstructorRejectsNullCascadePath() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new FaceDetector(null, null));
        assertTrue(e.getMessage().contains("cascadePath"));
    }

    @Test
    void testConstructorRejectsNullPreprocessor() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new FaceDetector("path/to/cascade.xml", null));
        assertTrue(e.getMessage().contains("preprocessor"));
    }
}
