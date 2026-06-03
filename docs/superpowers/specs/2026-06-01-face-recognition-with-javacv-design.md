# JavaCV Face Recognition for Money-Transfer Web App

## 1. Overview

Replace external CompreFace (Docker REST API) with embedded **JavaCV (OpenCV wrapper)** for face
recognition in the money-transfer banking web application (Spring Boot + Thymeleaf).

**Goal:** Demonstrate Computer Vision theory (Haar Cascade, LBPH, color spaces) through a practical
banking app — maximize all 4 evaluation criteria (Functionality, Java Best Practices, Math Insight,
Documentation).

## 2. Architecture

```
User (Browser)
    │
    ├── Face Registration ──→ FaceService ──→ JavaCVFaceEngine
    │   (camera → base64)         │                ├── FaceDetector (Haar Cascade)
    │                              │                ├── FaceRecognizer (LBPH)
    │                              │                └── ImagePreprocessor (grayscale, resize, equalize)
    │                              │
    ├── Face Login ───────────────┤
    │   (camera → base64)          │
    │                              │
    └── Transfer > 10M ───────────┘
         (verify face → faceToken → transfer)
```

### Packages

```
com.moneytransfer.face
├── JavaCVFaceEngine.java       # Facade: detect + recognize + train
├── FaceDetector.java           # Haar Cascade wrapper
├── FaceRecognizer.java         # LBPH wrapper
├── ImagePreprocessor.java      # Grayscale, histogram equalization, resize
├── LBPHModelManager.java       # Save/load trained model to disk
├── FaceService.java            # (modified) inject JavaCVFaceEngine instead of CompreFaceClient
└── FaceProcessingConfig.java   # (modified) init LBPH model + cascade XML on startup
```

## 3. OpenCV Theory (Math Insight — 20%)

### 3.1 Face Detection — Haar Cascade (Viola-Jones)

- **Haar-like features:** Edge, line, center-surround features (like "sum of white pixels — sum of black pixels")
- **Integral Image:** O(1) rectangle sum computation — key performance innovation
- **AdaBoost:** Selects best features from ~160k candidates, builds strong classifier
- **Cascade:** Rejects non-face regions early — 1st stage filters 50% negatives with only 2 features
- **Scale space:** Image pyramid with `scaleFactor=1.1`

### 3.2 Face Recognition — LBPH

- **Local Binary Pattern:** For each pixel, compare to 8 neighbors → binary string → decimal (0-255)
- **Histogram:** Divide face into 8x8 grid, compute LBP histogram per cell
- **Chi-square distance:** Compare histograms between query and training faces
- **Confidence threshold:** `predict()` returns (-1, -1) if distance > threshold

### 3.3 Image Preprocessing

- **Grayscale:** `cvtColor(BGR2GRAY)` — reduce 3 channels → 1, keep texture info
- **Histogram Equalization:** `equalizeHist()` — improve contrast for varying lighting
- **Resize:** 200x200 — normalize input size for LBPH

## 4. Java Best Practices (20%)

- **Strategy Pattern:** `FaceEngine` interface with `JavaCVFaceEngine` implementation
- **Singleton:** LBPH model + CascadeClassifier loaded once per app lifecycle
- **Dependency Injection:** Spring `@Component` + constructor injection
- **Separation of Concerns:** Controller → Service → Engine (3 layers)
- **Error Handling:** Custom `FaceException`, proper try-catch in controller
- **Thread Safety:** Face operations run on `faceExecutor` thread pool
- **File Structure:** Each class has single responsibility, <150 lines each

## 5. Functionality — Features (40%)

### 5.1 Face Registration
1. User clicks "Đăng ký gương mặt" on profile page
2. Camera opens via `getUserMedia()` → capture JPEG → base64
3. Backend receives base64 → `JavaCVFaceEngine.register(userId, imageBytes)`
4. Detect face ROI, crop, resize to 200x200

### 5.2 Face Login
1. User enters username, clicks "Đăng nhập bằng gương mặt"
2. Camera capture → backend verify → LBPH predict
3. If match: reset `failedAttempts`, generate JWT, redirect to dashboard
4. If fail: increment `failedAttempts`, lock account at 5 failures

### 5.3 Face Verification for Transfers > 10M VND
1. User enters transfer amount > 10M, clicks "Chuyển tiền"
2. Form detects amount > threshold → opens camera
3. Backend verifies face → returns `faceToken` (JWT, 60s expiry)
4. Token included in transfer request → validated by `TransactionController`
5. Double validation: ownership (`faceUserId == userId`) + purpose (`"TRANSFER"`)

### 5.4 Scheduled/Recurring Transfers with Face Verify (new)
1. User sets up recurring transfer (daily/weekly/monthly)
2. First execution: face verify required
3. Subsequent executions: reuse face token within expiry, or re-verify
4. Cron job triggers transfer only if face token is valid

## 6. Documentation — Slides Structure (20%)

### Slide 1-2: Problem & Solution
- Banking app needs security for high-value transactions
- Solution: Face recognition + LBPH + Haar Cascade

### Slide 3-5: Theory (Math Insight)
- Haar Cascade: integral image, AdaBoost, cascade structure
- LBPH: local binary patterns, histogram matching, chi-square
- Color spaces: BGR → GRAY, histogram equalization

### Slide 6-8: Architecture (Java Best Practices)
- Class diagram (UML): Controller → Service → JavaCVFaceEngine
- Strategy pattern: FaceEngine interface
- Sequence diagram: face login flow

### Slide 9-10: Demo (Functionality)
- Screenshots: face registration, face login, transfer verification
- Code snippets: detectMultiScale, LBPH.predict, equalizeHist

### Slide 11: Conclusion
- Results, performance stats (detection time, confidence scores)
- Lessons learned

## 7. Files to Modify / Create

### Modify (in money-transfer project)
| File | Change |
|---|---|
| `pom.xml` | Add javacv-platform dependency |
| `FaceService.java` | Inject `JavaCVFaceEngine` |
| `FaceProcessingConfig.java` | Add CascadeClassifier + LBPH model init |
| `FaceController.java` | No change needed (interface stays same) |
| `AuthController.java` | No change needed |
| `TransactionController.java` | Already fixed (toAccountNumber bug) |

### Create new
| File | Responsibility |
|---|---|
| `JavaCVFaceEngine.java` | Facade — register, verify, predict |
| `FaceDetector.java` | Haar cascade detect |
| `FaceRecognizer.java` | LBPH train + predict |
| `ImagePreprocessor.java` | Grayscale, equalizeHist, resize |
| `LBPHModelManager.java` | Save/load model.yml to disk |
| `RecurringTransferService.java` | Scheduled transfers with face verify |

## 8. Dependency

```xml
<dependency>
    <groupId>org.bytedeco</groupId>
    <artifactId>javacv-platform</artifactId>
    <version>1.5.10</version>
</dependency>
```

OpenCV cascade XML files (`haarcascade_frontalface_default.xml`) bundled in `javacv-platform`
jar, accessible via classpath.

LBPH model saved to `~/.money-transfer/face-model.xml` on train, loaded on startup (OpenCV FaceRecognizer.save() uses XML format).

## 9. Non-Goals

- Real-time video stream processing (frame-by-frame only on capture)
- Deep learning (DL4J) — focus on classical CV algorithms for Math Insight
- Browser-based JS face detection — all detection runs on server via JavaCV
- Multiple face support — one face per user, one user per verification
