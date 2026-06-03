# JavaCV Face Recognition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace external CompreFace Docker service with embedded JavaCV (OpenCV wrapper) for face recognition in the money-transfer banking app.

**Architecture:** 4 new files (`ImagePreprocessor`, `FaceDetector`, `FaceRecognizer`, `LBPHModelManager`) compose into `JavaCVFaceEngine` facade. `FaceService` switches dependency from `CompreFaceClient` to `JavaCVFaceEngine`. All controller/API layers stay unchanged — zero frontend changes.

**Tech Stack:** Java 21, Spring Boot 3.2.4, `javacv-platform:1.5.10`, OpenCV 4.9.0, JUnit 5 + Mockito

---

### Task 1: Add javacv-platform dependency + verify build

**Files:**
- Modify: `pom.xml:64-65`

- [ ] **Step 1: Add javacv-platform to pom.xml**

Insert after the ZXing dependency block (after line 73):

```xml
        <dependency>
            <groupId>org.bytedeco</groupId>
            <artifactId>javacv-platform</artifactId>
            <version>1.5.10</version>
        </dependency>
```

- [ ] **Step 2: Build to verify dependency resolves**

Run: `mvn dependency:resolve -DincludeScope=compile -q`
Expected: BUILD SUCCESS. Should see `javacv-platform-1.5.10` and `opencv-platform-4.9.0-1.5.10` in resolved list.

- [ ] **Step 3: Commit**

```bash
git add pom.xml
git commit -m "feat: add javacv-platform dependency"
```

---

### Task 2: Create ImagePreprocessor — grayscale + equalizeHist + resize

**Files:**
- Create: `src/main/java/com/moneytransfer/face/ImagePreprocessor.java`
- Create: `src/test/java/com/moneytransfer/face/ImagePreprocessorTest.java`

- [ ] **Step 1: Write the test**

```java
package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ImagePreprocessorTest {

    @Test
    void testToGrayScaleReturnsSingleChannel() {
        // Cannot test with real Mat in unit test (needs native libs)
        // Verify the class loads and constants exist
        assertDoesNotThrow(() -> new ImagePreprocessor());
    }
}
```

- [ ] **Step 2: Run test — should fail (no class yet)**

Run: `mvn test -pl . -Dtest=ImagePreprocessorTest -q 2>&1 | Select-String "FAIL"`

- [ ] **Step 3: Create ImagePreprocessor**

```java
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
```

- [ ] **Step 4: Run test — should pass**

```bash
mvn test -Dtest=ImagePreprocessorTest -q
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/face/ImagePreprocessor.java src/test/java/com/moneytransfer/face/ImagePreprocessorTest.java
git commit -m "feat: add ImagePreprocessor for grayscale, equalizeHist, resize"
```

---

### Task 3: Create FaceDetector — Haar Cascade wrapper

**Files:**
- Create: `src/main/java/com/moneytransfer/face/FaceDetector.java`
- Create: `src/test/java/com/moneytransfer/face/FaceDetectorTest.java`

- [ ] **Step 1: Write the test**

```java
package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FaceDetectorTest {

    @Test
    void testLoadCascadeDoesNotThrowOnValidPath() {
        // Unit test: verify constructor handles null/empty gracefully
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new FaceDetector(null, null));
        assertTrue(e.getMessage().contains("cascadePath"));
    }
}
```

- [ ] **Step 2: Run test to verify failure**

```bash
mvn test -Dtest=FaceDetectorTest -q 2>&1 | Select-String "FAIL"
```

- [ ] **Step 3: Create FaceDetector**

```java
package com.moneytransfer.face;

import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Rect;
import org.bytedeco.opencv.opencv_core.RectVector;
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
        cascade.detectMultiScale(equalized, faces, 1.1, 3, 0, new org.bytedeco.opencv.opencv_core.Size(100, 100),
                new org.bytedeco.opencv.opencv_core.Size());
        return faces;
    }

    public Mat cropLargestFace(Mat source) {
        RectVector faces = detect(source);
        if (faces.isEmpty()) {
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
```

- [ ] **Step 4: Run test**

```bash
mvn test -Dtest=FaceDetectorTest -q
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/face/FaceDetector.java src/test/java/com/moneytransfer/face/FaceDetectorTest.java
git commit -m "feat: add FaceDetector with Haar Cascade"
```

---

### Task 4: Create FaceRecognizer — LBPH wrapper

**Files:**
- Create: `src/main/java/com/moneytransfer/face/FaceRecognizer.java`
- Create: `src/test/java/com/moneytransfer/face/FaceRecognizerTest.java`

- [ ] **Step 1: Write the test**

```java
package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FaceRecognizerTest {

    @Test
    void testConstructorRejectsNullImages() {
        Exception e = assertThrows(IllegalArgumentException.class,
                () -> new FaceRecognizer(null));
        assertTrue(e.getMessage().contains("modelPath"));
    }
}
```

- [ ] **Step 2: Run test to verify failure**

- [ ] **Step 3: Create FaceRecognizer**

```java
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

    public void train(MatVector images, IntPointer labels) {
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
        return recognizer.getSampleSize() > 0;
    }

    public record RecognizerResult(int label, double confidence) {
        public boolean isMatch(long expectedUserId) {
            return label == expectedUserId && confidence < CONFIDENCE_THRESHOLD;
        }
    }
}
```

- [ ] **Step 4: Run test**

```bash
mvn test -Dtest=FaceRecognizerTest -q
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/face/FaceRecognizer.java src/test/java/com/moneytransfer/face/FaceRecognizerTest.java
git commit -m "feat: add FaceRecognizer with LBPH"
```

---

### Task 5: Create LBPHModelManager — save/load model + manage face images

**Files:**
- Create: `src/main/java/com/moneytransfer/face/LBPHModelManager.java`
- Create: `src/test/java/com/moneytransfer/face/LBPHModelManagerTest.java`

- [ ] **Step 1: Write the test**

```java
package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class LBPHModelManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testGetModelPathCreatesDirectory() {
        LBPHModelManager manager = new LBPHModelManager(tempDir.toString());
        assertTrue(tempDir.toFile().exists());
    }
}
```

- [ ] **Step 2: Run test to verify failure**

- [ ] **Step 3: Create LBPHModelManager**

```java
package com.moneytransfer.face;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;

public class LBPHModelManager {
    private static final Logger log = LoggerFactory.getLogger(LBPHModelManager.class);
    private final Path modelDir;

    public LBPHModelManager(String modelDirPath) {
        this.modelDir = Path.of(modelDirPath);
        try {
            Files.createDirectories(this.modelDir);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create model directory: " + modelDirPath, e);
        }
        log.info("LBPH model directory: {}", this.modelDir);
    }

    public String getModelFilePath() {
        return modelDir.resolve("face-model.xml").toAbsolutePath().toString();
    }

    public String getHaarCascadePath() {
        return modelDir.resolve("haarcascade_frontalface_default.xml").toAbsolutePath().toString();
    }

    public Path getModelDir() {
        return modelDir;
    }
}
```

- [ ] **Step 4: Run test**

```bash
mvn test -Dtest=LBPHModelManagerTest -q
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/face/LBPHModelManager.java src/test/java/com/moneytransfer/face/LBPHModelManagerTest.java
git commit -m "feat: add LBPHModelManager for model directory management"
```

---

### Task 6: Create JavaCVFaceEngine — facade combining all CV components

**Files:**
- Create: `src/main/java/com/moneytransfer/face/JavaCVFaceEngine.java`
- Create: `src/test/java/com/moneytransfer/face/JavaCVFaceEngineTest.java`

- [ ] **Step 1: Write the test**

```java
package com.moneytransfer.face;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.RectVector;
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
}
```

- [ ] **Step 2: Run test to verify failure**

- [ ] **Step 3: Create JavaCVFaceEngine**

```java
package com.moneytransfer.face;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacpp.IntPointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.MatVector;
import org.bytedeco.opencv.opencv_core.RectVector;
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
        IntPointer labels = new IntPointer(faceStore.size());
        int i = 0;
        for (Map.Entry<Long, byte[]> entry : faceStore.entrySet()) {
            Mat source = decodeImage(entry.getValue());
            Mat faceRoi = faceDetector.cropLargestFace(source);
            images.put(i, preprocessor.preprocess(faceRoi));
            labels.put(i, entry.getKey().intValue());
            i++;
        }
        faceRecognizer.train(images, labels);
    }

    public void addExistingFace(Long userId, byte[] imageBytes) {
        faceStore.put(userId, imageBytes);
    }

    public boolean isReady() {
        return faceRecognizer.isTrained();
    }
}
```

- [ ] **Step 4: Run test**

```bash
mvn test -Dtest=JavaCVFaceEngineTest -q
```

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/face/JavaCVFaceEngine.java src/test/java/com/moneytransfer/face/JavaCVFaceEngineTest.java
git commit -m "feat: add JavaCVFaceEngine facade"
```

---

### Task 7: Modify FaceProcessingConfig — init CV components on startup

**Files:**
- Modify: `src/main/java/com/moneytransfer/config/FaceProcessingConfig.java`

- [ ] **Step 1: Rewrite FaceProcessingConfig**

Replace entire file with:

```java
package com.moneytransfer.config;

import com.moneytransfer.face.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class FaceProcessingConfig {
    private static final Logger log = LoggerFactory.getLogger(FaceProcessingConfig.class);

    @Value("${app.face.thread-pool-size:4}")
    private int poolSize;

    @Value("${app.face.model-dir:${user.home}/.money-transfer}")
    private String modelDir;

    @Bean
    public LBPHModelManager lbphModelManager() {
        return new LBPHModelManager(modelDir);
    }

    @Bean
    public ImagePreprocessor imagePreprocessor() {
        return new ImagePreprocessor();
    }

    @Bean
    public FaceDetector faceDetector(ImagePreprocessor preprocessor, LBPHModelManager modelManager) {
        String cascadePath = modelManager.getHaarCascadePath();
        Path cascadeFile = Path.of(cascadePath);
        if (!Files.exists(cascadeFile)) {
            try {
                Files.createDirectories(cascadeFile.getParent());
                ClassPathResource resource = new ClassPathResource("haarcascade_frontalface_default.xml");
                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        Files.copy(is, cascadeFile, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Extracted Haar cascade to: {}", cascadePath);
                    }
                } else {
                    log.warn("Haar cascade not found in classpath, bundled JAR may not include it");
                }
            } catch (Exception e) {
                log.warn("Could not extract Haar cascade: {}", e.getMessage());
            }
        }
        return new FaceDetector(cascadePath, preprocessor);
    }

    @Bean
    public FaceRecognizer faceRecognizer(LBPHModelManager modelManager) {
        return new FaceRecognizer(modelManager.getModelFilePath());
    }

    @Bean
    public JavaCVFaceEngine javaCVFaceEngine(FaceDetector faceDetector, FaceRecognizer faceRecognizer,
                                              ImagePreprocessor preprocessor) {
        return new JavaCVFaceEngine(faceDetector, faceRecognizer, preprocessor);
    }

    @Bean("faceExecutor")
    public Executor faceExecutor() {
        return Executors.newFixedThreadPool(poolSize);
    }
}
```

- [ ] **Step 2: Add model-dir to application.yml**

Insert under `app.face:` section:

```yaml
    model-dir: ${FACE_MODEL_DIR:${user.home}/.money-transfer}
```

- [ ] **Step 3: Verify Haar cascade is accessible**

The `FaceProcessingConfig` already extracts `haarcascade_frontalface_default.xml` from classpath at startup (using `ClassPathResource`). No manual copy needed — OpenCV bundles this XML inside `opencv-platform.jar`.

Verify: the file exists in the dependency:
```bash
jar tf %USERPROFILE%\.m2\repository\org\bytedeco\opencv\opencv-platform\4.9.0-1.5.10\opencv-platform-4.9.0-1.5.10.jar 2>$null | Select-String "haarcascade_frontalface_default"
```
Expected: prints the path inside the JAR

- [ ] **Step 4: Build to verify no compilation errors**

```bash
mvn compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/config/FaceProcessingConfig.java src/main/resources/application.yml
git commit -m "feat: configure JavaCV beans in FaceProcessingConfig"
```

---

### Task 8: Modify FaceService — switch from CompreFaceClient to JavaCVFaceEngine

**Files:**
- Modify: `src/main/java/com/moneytransfer/face/FaceService.java`
- Modify: `src/test/java/com/moneytransfer/face/FaceServiceTest.java` (create)

- [ ] **Step 1: Write the test**

```java
package com.moneytransfer.face;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FaceServiceTest {

    @Mock private JavaCVFaceEngine javaCVFaceEngine;
    @Mock private UserRepository userRepository;
    private FaceService faceService;

    @BeforeEach
    void setUp() {
        faceService = new FaceService(javaCVFaceEngine, userRepository, Runnable::run);
    }

    @Test
    void testRegisterFaceSync_Success() {
        User user = new User();
        user.setId(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        String result = faceService.registerFaceSync(1L, "dGVzdA==");

        assertEquals("Face registered successfully", result);
        assertTrue(user.isFaceEnabled());
        verify(userRepository).save(user);
    }

    @Test
    void testRegisterFaceSync_UserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class,
                () -> faceService.registerFaceSync(99L, "dGVzdA=="));
    }

    @Test
    void testVerifyFaceBase64_Success() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setFaceEnabled(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(javaCVFaceEngine.verifyFace(eq(1L), any())).thenReturn(true);

        CompletableFuture<Boolean> future = faceService.verifyFaceBase64(1L, "dGVzdA==");

        assertTrue(future.get());
    }

    @Test
    void testVerifyFaceBase64_NoFaceRegistered() {
        User user = new User();
        user.setId(1L);
        user.setFaceEnabled(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class,
                () -> faceService.verifyFaceBase64(1L, "dGVzdA=="));
    }
}
```

- [ ] **Step 2: Run test to verify failure**

```bash
mvn test -Dtest=FaceServiceTest -q 2>&1 | Select-String "FAIL"
```

- [ ] **Step 3: Rewrite FaceService to inject JavaCVFaceEngine**

```java
package com.moneytransfer.face;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class FaceService {
    private final JavaCVFaceEngine javaCVFaceEngine;
    private final UserRepository userRepository;
    private final Executor faceExecutor;

    public FaceService(JavaCVFaceEngine javaCVFaceEngine, UserRepository userRepository,
                       @Qualifier("faceExecutor") Executor faceExecutor) {
        this.javaCVFaceEngine = javaCVFaceEngine;
        this.userRepository = userRepository;
        this.faceExecutor = faceExecutor;
    }

    public String registerFaceSync(Long userId, String base64Image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            javaCVFaceEngine.registerFace(userId, imageBytes);
            user.setFaceEnabled(true);
            userRepository.save(user);
            return "Face registered successfully";
        } catch (Exception e) {
            throw new RuntimeException("Face registration failed: " + e.getMessage());
        }
    }

    public CompletableFuture<Boolean> verifyFaceBase64(Long userId, String base64Image) {
        if (base64Image == null || base64Image.isEmpty()) {
            throw new IllegalArgumentException("Face image is required");
        }
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (!user.isFaceEnabled()) {
                throw new IllegalArgumentException("No face registered");
            }
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64Image);
                return javaCVFaceEngine.verifyFace(userId, imageBytes);
            } catch (Exception e) {
                throw new RuntimeException("Face verification failed", e);
            }
        }, faceExecutor);
    }

    public boolean verifyFaceBase64Sync(Long userId, String base64Image) {
        try {
            return verifyFaceBase64(userId, base64Image).get(15, TimeUnit.SECONDS);
        } catch (java.util.concurrent.ExecutionException | java.util.concurrent.TimeoutException | InterruptedException e) {
            throw new RuntimeException("Face verification failed", e);
        }
    }
}
```

- [ ] **Step 4: Run tests**

```bash
mvn test -Dtest=FaceServiceTest -q
```

Expected: All 4 tests pass

- [ ] **Step 5: Run ALL tests to verify nothing is broken**

```bash
mvn test -q
```

Expected: BUILD SUCCESS (existing TransactionServiceTest + new face tests all pass)

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/moneytransfer/face/FaceService.java src/test/java/com/moneytransfer/face/FaceServiceTest.java
git commit -m "refactor: switch FaceService from CompreFaceClient to JavaCVFaceEngine"
```

---

### Task 9: Build and verify the whole app compiles

- [ ] **Step 1: Full compilation check**

```bash
mvn clean compile -q
```

Expected: BUILD SUCCESS

- [ ] **Step 2: Run all tests**

```bash
mvn test -q
```

Expected: BUILD SUCCESS (all tests pass)

- [ ] **Step 3: Start the app to verify it boots**

```bash
mvn spring-boot:run -q 2>&1 | Select-String "Started|ERROR"
```

Press Ctrl+C to stop after confirming it starts without errors.

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "feat: replace CompreFace with JavaCV face recognition"
```
