package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FaceRecognizerTest {

    @Test
    void testConstructorRejectsNullModelPath() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new FaceRecognizer(null));
        assertTrue(e.getMessage().contains("modelPath"));
    }

    @Test
    void testConstructorRejectsEmptyModelPath() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new FaceRecognizer("  "));
        assertTrue(e.getMessage().contains("modelPath"));
    }
}
