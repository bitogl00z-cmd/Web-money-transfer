package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImagePreprocessorTest {

    @Test
    void testClassLoadsWithoutError() {
        assertDoesNotThrow(() -> new ImagePreprocessor());
    }
}
