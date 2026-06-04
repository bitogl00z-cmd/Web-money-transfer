package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LBPHModelManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testConstructorCreatesDirectory() {
        LBPHModelManager manager = new LBPHModelManager(tempDir.toString());
        assertTrue(tempDir.toFile().exists());
    }

    @Test
    void testGetModelFilePath() {
        LBPHModelManager manager = new LBPHModelManager(tempDir.toString());
        assertTrue(manager.getModelFilePath().endsWith("face-model.xml"));
    }

    @Test
    void testGetHaarCascadePath() {
        LBPHModelManager manager = new LBPHModelManager(tempDir.toString());
        assertTrue(manager.getHaarCascadePath().endsWith("haarcascade_frontalface_default.xml"));
    }
}
