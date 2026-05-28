# Banking UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign banking UI (blue & white), add VND currency, face login, and face verification for transfers > 10M VND.

**Architecture:** Backend gets 3 new endpoints (face-login, verify-for-transfer, face-token validation). All templates share a Thymeleaf layout fragment with professional CSS/JS. Face verification uses existing OpenCV pipeline.

**Tech Stack:** Spring Boot 3.2, Thymeleaf, OpenCV, Tailwind (via CDN for utility classes), custom banking.css, vanilla JS with getUserMedia API.

---

## File Map

### New Files
| File | Purpose |
|------|---------|
| `src/main/resources/static/css/banking.css` | Full banking design system |
| `src/main/resources/static/js/banking.js` | Shared JS (VND format, camera, modals, toast) |
| `src/main/resources/templates/fragments/layout.html` | Shared Thymeleaf layout (head, nav, sidebar, scripts fragments) |

### Modified Files
| File | Changes |
|------|---------|
| `FaceService.java` | Add `verifyFaceBase64(userId, base64)` method |
| `FaceController.java` | Add `POST /verify-for-transfer` endpoint |
| `AuthController.java` | Add `POST /face-login` endpoint |
| `TransactionController.java` | Validate faceToken for amount > 10M |
| `templates/login.html` | Full rewrite: layout + face scan button + camera modal |
| `templates/register.html` | Use layout, VND |
| `templates/dashboard.html` | Use layout, VND format, Chart.js |
| `templates/transfer.html` | Use layout, VND, face verify modal for >10M |
| `templates/history.html` | Use layout, VND |
| `templates/beneficiaries.html` | Use layout, VND |
| `templates/scheduled.html` | Use layout, VND |
| `templates/profile.html` | Use layout, face registration camera |
| `templates/admin/users.html` | Use layout, VND |
| `templates/admin/transactions.html` | Use layout, VND |

---

### Task 1: Backend — FaceService base64 verification

**Files:**
- Modify: `src/main/java/com/moneytransfer/face/FaceService.java`

Add a method that accepts base64 image string (for face-login and verify-for-transfer, where no MultipartFile is available):

- [ ] **Step 1: Add `verifyFaceBase64` to FaceService**

```java
// After existing verifyFace method
public CompletableFuture<Boolean> verifyFaceBase64(Long userId, String base64Image) {
    return CompletableFuture.supplyAsync(() -> {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.getFaceEncoding() == null) {
            throw new IllegalArgumentException("No face registered");
        }
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Image);
            MatOfByte matOfByte = new MatOfByte(imageBytes);
            Mat image = faceUtil.resizeToMax(Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR));
            Rect faceRect = faceUtil.detectFace(image);
            double[] encoding = faceUtil.computeEncoding(image, faceRect);
            double[] stored = Arrays.stream(user.getFaceEncoding().split(","))
                    .mapToDouble(Double::parseDouble).toArray();
            double similarity = faceUtil.cosineSimilarity(encoding, stored);
            return similarity >= similarityThreshold;
        } catch (Exception e) {
            throw new RuntimeException("Face verification failed: " + e.getMessage());
        }
    }, faceExecutor);
}
```

Add imports:
```java
import java.util.Base64;
```

- [ ] **Step 2: Compile to verify**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/face/FaceService.java
git commit -m "feat: add verifyFaceBase64 method to FaceService"
```

---

### Task 2: Backend — AuthController face-login endpoint

**Files:**
- Modify: `src/main/java/com/moneytransfer/auth/AuthController.java`
- Create: `src/main/java/com/moneytransfer/face/FaceLoginRequest.java`
- Modify: `src/main/java/com/moneytransfer/face/FaceService.java` (maybe)

Actually, I'll put the face-login in AuthController since it deals with authentication.

For face-login, the endpoint needs to:
1. Accept `{ username: string, faceImage: string (base64) }`
2. Find user by username
3. Call faceService.verifyFaceBase64
4. If match, generate JWT and set cookies (reuse AuthService.setAuthCookies)

But faceService.verifyFaceBase64 returns CompletableFuture<Boolean>, and the controller needs to wait for it. Let me handle this synchronously or use block on the future.

Actually, for REST API simplicity, I'll make it synchronous. The verifyFaceBase64 method runs async internally but I'll make a synchronous version for the auth flow. Or I can just call `.get()` on the CompletableFuture.

Wait, looking at the existing AuthController pattern — all endpoints are synchronous. The FaceController uses async because face operations can be slow. For login, a short delay is acceptable. Let me just add a synchronous helper.

Actually, the simplest approach: add a synchronous `verifyFaceBase64Sync` method to FaceService that wraps the async one:

- [ ] **Step 1: Add synchronous verify method to FaceService**

```java
public boolean verifyFaceBase64Sync(Long userId, String base64Image) {
    try {
        return verifyFaceBase64(userId, base64Image).get(15, TimeUnit.SECONDS);
    } catch (Exception e) {
        throw new RuntimeException("Face verification failed: " + e.getMessage());
    }
}
```

Add import: `import java.util.concurrent.TimeUnit;`

- [ ] **Step 2: Create FaceLoginRequest record**

File: `src/main/java/com/moneytransfer/face/FaceLoginRequest.java`

```java
package com.moneytransfer.face;

public record FaceLoginRequest(String username, String faceImage) {}
```

- [ ] **Step 3: Add face-login endpoint to AuthController**

Add endpoint before the `extractTokenFromCookies` private methods:

```java
@PostMapping("/face-login")
public ResponseEntity<?> faceLogin(@RequestBody FaceLoginRequest request,
                                    HttpServletRequest httpRequest,
                                    HttpServletResponse response) {
    try {
        String ip = httpRequest.getRemoteAddr();
        User user = userRepository.findByUsername(request.username())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (user.isLocked()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account is locked"));
        }
        if (!user.isFaceEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Face login not enabled. Register your face first."));
        }
        boolean matched = faceService.verifyFaceBase64Sync(user.getId(), request.faceImage());
        if (!matched) {
            user.setFailedAttempts(user.getFailedAttempts() + 1);
            if (user.getFailedAttempts() >= 5) {
                user.setLocked(true);
            }
            userRepository.save(user);
            return ResponseEntity.badRequest().body(Map.of("error", "Face verification failed"));
        }
        user.setFailedAttempts(0);
        userRepository.save(user);

        auditLogRepository.save(new AuditLog(user.getId(), "FACE_LOGIN", "Face login successful", ip));

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());
        authService.setAuthCookies(response, accessToken, refreshToken);

        return ResponseEntity.ok(Map.of("message", "Face login successful", "userId", user.getId()));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

Add required injections to AuthController constructor:
```java
private final FaceService faceService;
private final UserRepository userRepository;
private final AuditLogRepository auditLogRepository;
```

Update constructor signature. For existing `extractTokenFromCookies` and `extractRefreshTokenFromCookies` these stay as-is.

Note: AuthController already has `jwtUtil` and `authService` fields. Need to add:
```java
private final FaceService faceService;
private final UserRepository userRepository;
private final AuditLogRepository auditLogRepository;
```

And add these imports:
```java
import com.moneytransfer.face.FaceLoginRequest;
import com.moneytransfer.face.FaceService;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
```

- [ ] **Step 4: Compile to verify**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/auth/AuthController.java src/main/java/com/moneytransfer/face/FaceLoginRequest.java src/main/java/com/moneytransfer/face/FaceService.java
git commit -m "feat: add face-login endpoint"
```

---

### Task 3: Backend — FaceController verify-for-transfer

**Files:**
- Modify: `src/main/java/com/moneytransfer/face/FaceController.java`
- Modify: `src/main/java/com/moneytransfer/auth/JwtUtil.java`

- [ ] **Step 1: Add generateFaceToken to JwtUtil**

```java
public String generateFaceToken(Long userId, String purpose) {
    return Jwts.builder()
            .claim("userId", userId)
            .claim("purpose", purpose)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(secretKey)
            .compact();
}
```

- [ ] **Step 2: Add verify-face endpoint to FaceController**

```java
@PostMapping("/verify-for-transfer")
public CompletableFuture<ResponseEntity<Map<String, Object>>> verifyForTransfer(
        Authentication auth, @RequestBody Map<String, String> body) {
    Claims claims = (Claims) auth.getDetails();
    Long userId = ((Integer) claims.get("userId")).longValue();
    String faceImage = body.get("faceImage");
    if (faceImage == null) {
        return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "faceImage is required")));
    }
    return faceService.verifyFaceBase64(userId, faceImage)
            .thenApply(matched -> {
                if (matched) {
                    String faceToken = jwtUtil.generateFaceToken(userId, "TRANSFER");
                    return ResponseEntity.ok(Map.of("faceToken", (Object) faceToken));
                } else {
                    return ResponseEntity.badRequest().body(Map.of("error", (Object) "Face verification failed"));
                }
            })
            .exceptionally(e -> ResponseEntity.badRequest()
                    .body(Map.of("error", (Object) e.getCause().getMessage())));
}
```

Add `JwtUtil` injection to FaceController:
```java
private final JwtUtil jwtUtil;

// Update constructor
public FaceController(FaceService faceService, JwtUtil jwtUtil) {
    this.faceService = faceService;
    this.jwtUtil = jwtUtil;
}
```

- [ ] **Step 3: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/moneytransfer/auth/JwtUtil.java src/main/java/com/moneytransfer/face/FaceController.java
git commit -m "feat: add verify-for-transfer and face token generation"
```

---

### Task 4: Backend — TransactionController face check

**Files:**
- Modify: `src/main/java/com/moneytransfer/transaction/TransactionController.java`

- [ ] **Step 1: Add face token validation check to transfer endpoint**

Modify the transfer method to check amount > 10,000,000 and validate faceToken:

```java
@PostMapping("/transfer")
public ResponseEntity<?> transfer(@RequestBody Map<String, Object> body,
                                    Authentication auth, HttpServletRequest request) {
    try {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        Long fromId = Long.valueOf(body.get("fromAccountId").toString());
        Long toId = Long.valueOf(body.get("toAccountId").toString());
        BigDecimal amount = new BigDecimal(body.get("amount").toString());
        String description = (String) body.getOrDefault("description", "");

        // Face verification check for amounts > 10,000,000
        BigDecimal threshold = new BigDecimal("10000000");
        if (amount.compareTo(threshold) > 0) {
            String faceToken = (String) body.get("faceToken");
            if (faceToken == null || faceToken.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Face verification required for amounts over 10,000,000₫",
                    "faceRequired", true
                ));
            }
            try {
                Claims faceClaims = jwtUtil.validateToken(faceToken);
                Long faceUserId = ((Integer) faceClaims.get("userId")).longValue();
                String purpose = faceClaims.get("purpose", String.class);
                if (!faceUserId.equals(userId) || !"TRANSFER".equals(purpose)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Invalid face verification token"));
                }
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("error", "Face verification token expired or invalid"));
            }
        }

        String ip = request.getRemoteAddr();
        Transaction tx = transactionService.transfer(fromId, toId, amount, description, userId, ip);
        return ResponseEntity.ok(Map.of("transactionCode", tx.getTransactionCode(), "status", tx.getStatus()));
    } catch (IllegalArgumentException | IllegalStateException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

Add JwtUtil injection:
```java
private final JwtUtil jwtUtil;

// Update constructor
public TransactionController(TransactionService transactionService, AccountService accountService, JwtUtil jwtUtil) {
    this.transactionService = transactionService;
    this.accountService = accountService;
    this.jwtUtil = jwtUtil;
}
```

- [ ] **Step 2: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/transaction/TransactionController.java
git commit -m "feat: add face token validation for transfers over 10M VND"
```

---

### Task 5: Frontend — banking.css

**Files:**
- Create: `src/main/resources/static/css/banking.css`

- [ ] **Step 1: Create banking.css**

```css
:root {
    --primary-dark: #1e3a5f;
    --primary: #2563eb;
    --primary-light: #3b82f6;
    --primary-bg: #eff6ff;
    --bg: #f0f4f8;
    --card: #ffffff;
    --text: #1e293b;
    --text-muted: #64748b;
    --success: #10b981;
    --danger: #ef4444;
    --warning: #f59e0b;
    --border: #e2e8f0;
    --radius: 16px;
    --radius-sm: 8px;
    --shadow: 0 4px 24px rgba(0,0,0,0.08);
    --shadow-lg: 0 8px 40px rgba(0,0,0,0.12);
    --transition: 0.2s ease;
    --sidebar-width: 240px;
    --nav-height: 64px;
}

* { box-sizing: border-box; margin: 0; padding: 0; }

body {
    font-family: 'Inter', system-ui, -apple-system, sans-serif;
    background: var(--bg);
    color: var(--text);
    min-height: 100vh;
}

/* Navbar */
.navbar {
    position: fixed; top: 0; left: 0; right: 0;
    height: var(--nav-height);
    background: linear-gradient(135deg, var(--primary-dark), var(--primary));
    color: #fff;
    display: flex; align-items: center;
    padding: 0 24px;
    z-index: 100;
    box-shadow: 0 2px 12px rgba(0,0,0,0.15);
}
.navbar .logo {
    font-size: 1.25rem; font-weight: 700;
    display: flex; align-items: center; gap: 10px;
}
.navbar .logo-icon { font-size: 1.5rem; }
.navbar .nav-right { margin-left: auto; display: flex; align-items: center; gap: 16px; }
.navbar .nav-link { color: rgba(255,255,255,0.85); text-decoration: none; font-size: 0.9rem; transition: var(--transition); }
.navbar .nav-link:hover { color: #fff; }

/* Sidebar */
.sidebar {
    position: fixed; top: var(--nav-height); left: 0; bottom: 0;
    width: var(--sidebar-width);
    background: var(--card);
    border-right: 1px solid var(--border);
    padding: 16px 0;
    overflow-y: auto;
    z-index: 50;
    transition: transform var(--transition);
}
.sidebar-item {
    display: flex; align-items: center; gap: 12px;
    padding: 12px 24px;
    color: var(--text-muted);
    text-decoration: none;
    font-size: 0.9rem;
    transition: var(--transition);
    border-left: 3px solid transparent;
}
.sidebar-item:hover { background: var(--primary-bg); color: var(--primary); }
.sidebar-item.active { background: var(--primary-bg); color: var(--primary); border-left-color: var(--primary); font-weight: 600; }
.sidebar-item .icon { font-size: 1.1rem; width: 24px; text-align: center; }

/* Main content */
.main-content {
    margin-left: var(--sidebar-width);
    margin-top: var(--nav-height);
    padding: 32px;
    min-height: calc(100vh - var(--nav-height));
}

/* Cards */
.card {
    background: var(--card);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 24px;
    transition: var(--transition);
}
.card:hover { box-shadow: var(--shadow-lg); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 1.1rem; font-weight: 700; color: var(--text); }
.card-subtitle { font-size: 0.85rem; color: var(--text-muted); }

/* Stat cards */
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 32px; }
.stat-card {
    background: var(--card);
    border-radius: var(--radius);
    box-shadow: var(--shadow);
    padding: 20px 24px;
    transition: var(--transition);
    position: relative; overflow: hidden;
}
.stat-card::before {
    content: ''; position: absolute; top: 0; left: 0; right: 0; height: 4px;
}
.stat-card.blue::before { background: var(--primary); }
.stat-card.green::before { background: var(--success); }
.stat-card.orange::before { background: var(--warning); }
.stat-card.purple::before { background: #8b5cf6; }
.stat-card:hover { box-shadow: var(--shadow-lg); transform: translateY(-2px); }
.stat-label { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 4px; }
.stat-value { font-size: 1.75rem; font-weight: 700; color: var(--text); }
.stat-sub { font-size: 0.8rem; color: var(--text-muted); margin-top: 4px; }

/* Buttons */
.btn {
    display: inline-flex; align-items: center; justify-content: center; gap: 8px;
    padding: 10px 24px;
    border-radius: var(--radius-sm);
    font-size: 0.9rem; font-weight: 600;
    border: none; cursor: pointer;
    transition: var(--transition);
    text-decoration: none;
}
.btn-primary { background: var(--primary); color: #fff; }
.btn-primary:hover { background: var(--primary-dark); transform: translateY(-1px); }
.btn-success { background: var(--success); color: #fff; }
.btn-success:hover { background: #059669; transform: translateY(-1px); }
.btn-danger { background: var(--danger); color: #fff; }
.btn-danger:hover { background: #dc2626; }
.btn-outline { background: transparent; color: var(--primary); border: 1.5px solid var(--primary); }
.btn-outline:hover { background: var(--primary-bg); }
.btn-sm { padding: 6px 16px; font-size: 0.85rem; }
.btn-lg { padding: 14px 32px; font-size: 1rem; }
.btn-full { width: 100%; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }

/* Forms */
.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: 0.85rem; font-weight: 600; color: var(--text); margin-bottom: 6px; }
.form-input {
    width: 100%; padding: 10px 14px;
    border: 1.5px solid var(--border);
    border-radius: var(--radius-sm);
    font-size: 0.9rem;
    transition: var(--transition);
    outline: none;
    background: var(--card);
}
.form-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px rgba(37,99,235,0.15); }
.form-input.error { border-color: var(--danger); }
.form-select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2364748b' d='M6 8L1 3h10z'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 12px center; padding-right: 36px; }

/* Tables */
.table-container { overflow-x: auto; }
.table {
    width: 100%; border-collapse: collapse;
    font-size: 0.9rem;
}
.table th {
    text-align: left; padding: 12px 16px;
    font-weight: 600; color: var(--text-muted); font-size: 0.8rem;
    text-transform: uppercase; letter-spacing: 0.5px;
    border-bottom: 2px solid var(--border);
    background: var(--bg);
}
.table td { padding: 12px 16px; border-bottom: 1px solid var(--border); }
.table tr:hover td { background: var(--primary-bg); }
.table .empty { text-align: center; padding: 40px; color: var(--text-muted); }

/* Badges */
.badge {
    display: inline-block; padding: 3px 10px;
    border-radius: 20px; font-size: 0.75rem; font-weight: 600;
}
.badge-success { background: #d1fae5; color: #065f46; }
.badge-danger { background: #fce4ec; color: #b91c1c; }
.badge-warning { background: #fef3c7; color: #92400e; }
.badge-info { background: #dbeafe; color: #1e40af; }
.badge-purple { background: #ede9fe; color: #5b21b6; }
.badge-gray { background: #f1f5f9; color: #475569; }

/* Modal */
.modal-overlay {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
    display: flex; align-items: center; justify-content: center;
    z-index: 1000;
    animation: fadeIn 0.2s;
}
.modal {
    background: var(--card);
    border-radius: var(--radius);
    box-shadow: var(--shadow-lg);
    padding: 32px;
    max-width: 480px; width: 90%;
    max-height: 90vh; overflow-y: auto;
    animation: slideUp 0.3s;
}
.modal-title { font-size: 1.2rem; font-weight: 700; margin-bottom: 8px; }
.modal-desc { font-size: 0.9rem; color: var(--text-muted); margin-bottom: 20px; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 20px; }

/* Camera view */
.camera-container { position: relative; width: 100%; max-width: 360px; margin: 0 auto; border-radius: var(--radius-sm); overflow: hidden; background: #000; }
.camera-container video { width: 100%; display: block; }
.camera-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; border: 3px solid var(--primary); border-radius: var(--radius-sm); pointer-events: none; }
.camera-btn { position: absolute; bottom: 16px; left: 50%; transform: translateX(-50%); }

/* Toast */
.toast-container { position: fixed; top: 80px; right: 20px; z-index: 2000; display: flex; flex-direction: column; gap: 8px; }
.toast {
    padding: 12px 20px; border-radius: var(--radius-sm);
    color: #fff; font-size: 0.9rem; font-weight: 500;
    box-shadow: var(--shadow-lg);
    animation: slideIn 0.3s;
    display: flex; align-items: center; gap: 8px;
}
.toast-success { background: var(--success); }
.toast-error { background: var(--danger); }
.toast-info { background: var(--primary); }

/* Page headings */
.page-header { margin-bottom: 24px; }
.page-title { font-size: 1.5rem; font-weight: 700; color: var(--text); }
.page-subtitle { font-size: 0.9rem; color: var(--text-muted); margin-top: 4px; }

/* Grid helpers */
.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
@media (max-width: 768px) {
    .grid-2, .grid-3 { grid-template-columns: 1fr; }
}

/* Responsive */
@media (max-width: 768px) {
    .sidebar { transform: translateX(-100%); }
    .sidebar.open { transform: translateX(0); }
    .main-content { margin-left: 0; padding: 20px; }
    .navbar .hamburger { display: block !important; }
    .stat-grid { grid-template-columns: 1fr; }
}
@media (min-width: 769px) { .navbar .hamburger { display: none !important; } }

/* Animations */
@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }

/* Chart container */
.chart-container { position: relative; height: 300px; }

/* Loading spinner */
.spinner { width: 24px; height: 24px; border: 3px solid var(--border); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "feat: add banking.css design system"
```

---

### Task 6: Frontend — banking.js

**Files:**
- Create: `src/main/resources/static/js/banking.js`

- [ ] **Step 1: Create banking.js**

```javascript
// ===== VND Formatting =====
function formatVND(amount) {
    const num = Number(amount);
    if (isNaN(num)) return '0₫';
    return num.toLocaleString('vi-VN') + '₫';
}

// ===== Toast Notifications =====
function showToast(message, type) {
    type = type || 'success';
    const container = document.getElementById('toastContainer');
    if (!container) {
        const div = document.createElement('div');
        div.id = 'toastContainer';
        div.className = 'toast-container';
        document.body.appendChild(div);
    }
    const toast = document.createElement('div');
    toast.className = 'toast toast-' + type;
    const icons = { success: '✓', error: '✕', info: 'ℹ' };
    toast.innerHTML = '<span>' + (icons[type] || '') + '</span> ' + message;
    document.getElementById('toastContainer').appendChild(toast);
    setTimeout(function() { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.3s'; setTimeout(function() { toast.remove(); }, 300); }, 3000);
}

// ===== Modal =====
function openModal(html) {
    closeModal();
    var overlay = document.createElement('div');
    overlay.className = 'modal-overlay';
    overlay.id = 'activeModal';
    var modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = html;
    overlay.appendChild(modal);
    document.body.appendChild(overlay);
    overlay.addEventListener('click', function(e) { if (e.target === overlay) closeModal(); });
    return overlay;
}

function closeModal() {
    var existing = document.getElementById('activeModal');
    if (existing) { existing.remove(); }
}

// ===== Camera Capture =====
function openCamera(onCapture) {
    var html = '<div class="modal-title">📸 Xác thực gương mặt</div>' +
        '<div class="modal-desc">Vui lòng nhìn thẳng vào camera để chụp ảnh xác thực</div>' +
        '<div class="camera-container">' +
        '<video id="faceVideo" autoplay playsinline></video>' +
        '<div class="camera-overlay"></div>' +
        '</div>' +
        '<div class="modal-actions">' +
        '<button onclick="closeModal()" class="btn btn-outline">Hủy</button>' +
        '<button id="captureBtn" class="btn btn-primary">Chụp ảnh</button>' +
        '</div>';
    openModal(html);

    var video = document.getElementById('faceVideo');
    var stream = null;

    navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: 320, height: 240 } })
        .then(function(s) {
            stream = s;
            video.srcObject = s;
        })
        .catch(function() {
            showToast('Không thể truy cập camera', 'error');
            closeModal();
        });

    document.getElementById('captureBtn').addEventListener('click', function() {
        var canvas = document.createElement('canvas');
        canvas.width = video.videoWidth || 320;
        canvas.height = video.videoHeight || 240;
        var ctx = canvas.getContext('2d');
        ctx.drawImage(video, 0, 0);
        var base64 = canvas.toDataURL('image/jpeg', 0.8).split(',')[1];

        if (stream) { stream.getTracks().forEach(function(t) { t.stop(); }); }
        closeModal();

        if (onCapture) onCapture(base64);
    });
}

// ===== Date Formatting =====
function formatDate(dateStr) {
    if (!dateStr) return '';
    var d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' });
}

function formatDateShort(dateStr) {
    if (!dateStr) return '';
    var d = new Date(dateStr);
    return d.toLocaleDateString('vi-VN');
}

// ===== Fetch Helper =====
async function apiFetch(url, options) {
    options = options || {};
    options.credentials = 'include';
    options.headers = options.headers || {};
    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
        options.headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }
    try {
        var res = await fetch(url, options);
        var data = await res.json();
        return { ok: res.ok, status: res.status, data: data };
    } catch (e) {
        return { ok: false, status: 0, data: { error: 'Network error' } };
    }
}

// ===== Sidebar Toggle (Mobile) =====
function toggleSidebar() {
    document.querySelector('.sidebar').classList.toggle('open');
}

// Initialize sidebar close on main click (mobile)
document.addEventListener('DOMContentLoaded', function() {
    var sidebar = document.querySelector('.sidebar');
    var main = document.querySelector('.main-content');
    if (main && sidebar) {
        main.addEventListener('click', function() {
            if (window.innerWidth <= 768 && sidebar.classList.contains('open')) {
                sidebar.classList.remove('open');
            }
        });
    }
});
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/js/banking.js
git commit -m "feat: add banking.js shared utilities"
```

---

### Task 7: Frontend — Layout Fragment

**Files:**
- Create: `src/main/resources/templates/fragments/layout.html`

- [ ] **Step 1: Create layout.html with fragments**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:fragment="head(title)">
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title th:text="${title} + ' - Ngân Hàng'">Banking</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <script src="https://cdn.tailwindcss.com"></script>
    <link rel="stylesheet" th:href="@{/css/banking.css}">
</head>

<body th:fragment="navbar(currentPage)">
    <nav class="navbar">
        <button class="hamburger btn" onclick="toggleSidebar()" style="background:none;border:none;color:#fff;font-size:1.5rem;padding:4px 8px;margin-right:12px;">☰</button>
        <div class="logo">
            <span class="logo-icon">🏦</span>
            <span>Ngân Hàng</span>
        </div>
        <div class="nav-right">
            <a th:href="@{/profile}" class="nav-link" title="Hồ sơ">👤 <span th:text="${#authentication.name}">user</span></a>
            <a th:href="@{/logout}" class="nav-link" style="color:rgba(255,255,255,0.7)">🚪 Đăng xuất</a>
        </div>
    </nav>
</body>

<div th:fragment="sidebar(currentPage)">
    <aside class="sidebar">
        <a th:href="@{/dashboard}" th:classappend="${currentPage == 'dashboard'} ? 'active'" class="sidebar-item">
            <span class="icon">📊</span> Tổng quan
        </a>
        <a th:href="@{/transfer}" th:classappend="${currentPage == 'transfer'} ? 'active'" class="sidebar-item">
            <span class="icon">💸</span> Chuyển tiền
        </a>
        <a th:href="@{/history}" th:classappend="${currentPage == 'history'} ? 'active'" class="sidebar-item">
            <span class="icon">📋</span> Lịch sử
        </a>
        <a th:href="@{/beneficiaries}" th:classappend="${currentPage == 'beneficiaries'} ? 'active'" class="sidebar-item">
            <span class="icon">👥</span> Thụ hưởng
        </a>
        <a th:href="@{/scheduled}" th:classappend="${currentPage == 'scheduled'} ? 'active'" class="sidebar-item">
            <span class="icon">⏰</span> Định kỳ
        </a>
        <a th:href="@{/profile}" th:classappend="${currentPage == 'profile'} ? 'active'" class="sidebar-item">
            <span class="icon">👤</span> Hồ sơ
        </a>
        <a sec:authorize="hasRole('ADMIN')" th:href="@{/admin/users}" th:classappend="${currentPage == 'admin'} ? 'active'" class="sidebar-item">
            <span class="icon">🔧</span> Quản trị
        </a>
        <hr style="border:none;border-top:1px solid var(--border);margin:12px 16px;">
        <a th:href="@{/logout}" class="sidebar-item" style="color:var(--danger)">
            <span class="icon">🚪</span> Đăng xuất
        </a>
    </aside>
</div>

<div th:fragment="scripts">
    <script th:src="@{/js/banking.js}"></script>
</div>

<div th:fragment="toast">
    <div id="toastContainer" class="toast-container"></div>
</div>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/fragments/layout.html
git commit -m "feat: add shared layout fragments"
```

---

### Task 8: Frontend — Login page with face scan

**Files:**
- Modify: `src/main/resources/templates/login.html`

- [ ] **Step 1: Rewrite login.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Đăng nhập')"></head>
<body style="background:linear-gradient(135deg, var(--primary-dark), var(--primary));min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;">
    <div style="background:var(--card);border-radius:var(--radius);box-shadow:var(--shadow-lg);padding:40px;max-width:420px;width:100%;">
        <div style="text-align:center;margin-bottom:32px;">
            <div style="font-size:3rem;margin-bottom:8px;">🏦</div>
            <h1 style="font-size:1.5rem;font-weight:700;color:var(--text);">Ngân Hàng</h1>
            <p style="color:var(--text-muted);font-size:0.9rem;margin-top:4px;">Đăng nhập để tiếp tục</p>
        </div>

        <div id="errorMsg" style="display:none;background:#fce4ec;color:var(--danger);padding:12px 16px;border-radius:var(--radius-sm);font-size:0.85rem;margin-bottom:16px;"></div>

        <form id="loginForm">
            <div class="form-group">
                <label class="form-label">Tên đăng nhập</label>
                <input type="text" id="username" class="form-input" placeholder="Nhập username" required>
            </div>
            <div class="form-group">
                <label class="form-label">Mật khẩu</label>
                <input type="password" id="password" class="form-input" placeholder="••••••••" required>
            </div>
            <button type="submit" id="loginBtn" class="btn btn-primary btn-lg btn-full">Đăng nhập</button>
        </form>

        <div style="text-align:center;margin:20px 0;color:var(--text-muted);font-size:0.85rem;">— hoặc —</div>

        <button id="faceLoginBtn" class="btn btn-outline btn-lg btn-full" onclick="openFaceLogin()">
            📸 Đăng nhập bằng gương mặt
        </button>

        <p style="text-align:center;margin-top:24px;font-size:0.85rem;color:var(--text-muted);">
            Chưa có tài khoản? <a href="/register" style="color:var(--primary);font-weight:600;text-decoration:none;">Đăng ký</a>
        </p>
    </div>

    <script th:src="@{/js/banking.js}"></script>
    <script>
        document.getElementById('loginForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            var btn = document.getElementById('loginBtn');
            var err = document.getElementById('errorMsg');
            btn.disabled = true; btn.textContent = 'Đang đăng nhập...';
            err.style.display = 'none';
            var res = await apiFetch('/api/auth/login', {
                method: 'POST',
                body: { username: document.getElementById('username').value, password: document.getElementById('password').value }
            });
            if (res.ok) {
                window.location.href = '/dashboard';
            } else {
                err.textContent = res.data.error || 'Đăng nhập thất bại';
                err.style.display = 'block';
            }
            btn.disabled = false; btn.textContent = 'Đăng nhập';
        });

        async function openFaceLogin() {
            openCamera(async function(base64) {
                var btn = document.getElementById('faceLoginBtn');
                btn.disabled = true; btn.textContent = 'Đang xác thực...';
                var res = await apiFetch('/api/auth/face-login', {
                    method: 'POST',
                    body: { username: document.getElementById('username').value, faceImage: base64 }
                });
                if (res.ok) {
                    window.location.href = '/dashboard';
                } else {
                    showToast(res.data.error || 'Xác thực thất bại', 'error');
                }
                btn.disabled = false; btn.textContent = '📸 Đăng nhập bằng gương mặt';
            });
        }
    </script>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/login.html
git commit -m "feat: redesign login page with face login"
```

---

### Task 9: Frontend — Dashboard page

**Files:**
- Modify: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: Rewrite dashboard.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="fragments/layout :: head('Tổng quan')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('dashboard')"></nav>
    <aside th:replace="fragments/layout :: sidebar('dashboard')"></aside>

    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">📊 Tổng quan tài khoản</h1>
            <p class="page-subtitle">Xem tổng quan số dư và giao dịch gần đây</p>
        </div>

        <div class="stat-grid" id="statsContainer">
            <div class="stat-card blue">
                <div class="stat-label">Tổng số dư</div>
                <div class="stat-value" id="totalBalance">—</div>
                <div class="stat-sub">Tất cả tài khoản</div>
            </div>
            <div class="stat-card green">
                <div class="stat-label">Số tài khoản</div>
                <div class="stat-value" id="accountCount">—</div>
                <div class="stat-sub">Đang hoạt động</div>
            </div>
            <div class="stat-card orange">
                <div class="stat-label">Giao dịch gần đây</div>
                <div class="stat-value" id="recentCount">—</div>
                <div class="stat-sub">Trong 7 ngày qua</div>
            </div>
        </div>

        <div class="card" style="margin-bottom:24px;">
            <div class="card-header">
                <div>
                    <div class="card-title">Biểu đồ số dư 7 ngày</div>
                    <div class="card-subtitle">Biến động số dư theo ngày</div>
                </div>
            </div>
            <div class="chart-container">
                <canvas id="balanceChart"></canvas>
            </div>
        </div>

        <div class="card">
            <div class="card-header">
                <div>
                    <div class="card-title">Giao dịch gần đây</div>
                    <div class="card-subtitle">Các giao dịch mới nhất</div>
                </div>
                <a href="/history" class="btn btn-outline btn-sm">Xem tất cả</a>
            </div>
            <div class="table-container">
                <table class="table">
                    <thead>
                        <tr><th>Mã GD</th><th>Số tiền</th><th>Loại</th><th>Ngày</th></tr>
                    </thead>
                    <tbody id="txBody"></tbody>
                </table>
            </div>
        </div>
    </main>

    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <script>
        fetch('/api/dashboard/stats', { credentials: 'include' })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                document.getElementById('totalBalance').textContent = formatVND(data.totalBalance);
                document.getElementById('accountCount').textContent = data.accountCount;
                document.getElementById('recentCount').textContent = (data.recentTransactions && data.recentTransactions.length) || 0;
                var txBody = document.getElementById('txBody');
                if (data.recentTransactions && data.recentTransactions.length) {
                    data.recentTransactions.forEach(function(tx) {
                        var row = txBody.insertRow();
                        row.innerHTML = '<td style="font-family:monospace;font-size:0.85rem;">' + tx.transactionCode + '</td>'
                            + '<td style="font-weight:600;">' + formatVND(tx.amount) + '</td>'
                            + '<td><span class="badge badge-info">' + tx.type + '</span></td>'
                            + '<td>' + formatDateShort(tx.createdAt) + '</td>';
                    });
                } else {
                    txBody.innerHTML = '<tr><td colspan="4" class="empty">Chưa có giao dịch nào</td></tr>';
                }
                if (data.chartData) {
                    new Chart(document.getElementById('balanceChart'), {
                        type: 'line',
                        data: {
                            labels: data.chartData.map(function(d) { return d.date; }),
                            datasets: [{
                                label: 'Số dư ròng',
                                data: data.chartData.map(function(d) { return d.net; }),
                                borderColor: '#2563eb',
                                backgroundColor: 'rgba(37,99,235,0.1)',
                                fill: true,
                                tension: 0.3
                            }]
                        },
                        options: {
                            responsive: true, maintainAspectRatio: false,
                            plugins: { legend: { display: false } },
                            scales: { y: { ticks: { callback: function(v) { return formatVND(v); } } } }
                        }
                    });
                }
            })
            .catch(function() { showToast('Không thể tải dữ liệu', 'error'); });
    </script>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/dashboard.html
git commit -m "feat: redesign dashboard with VND and layout"
```

---

### Task 10: Frontend — Transfer page with face verify

**Files:**
- Modify: `src/main/resources/templates/transfer.html`

- [ ] **Step 1: Rewrite transfer.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Chuyển tiền')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('transfer')"></nav>
    <aside th:replace="fragments/layout :: sidebar('transfer')"></aside>

    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">💸 Chuyển tiền</h1>
            <p class="page-subtitle">Chuyển tiền đến tài khoản khác nhanh chóng và an toàn</p>
        </div>

        <div class="card" style="max-width:520px;">
            <div id="message" style="display:none;padding:12px 16px;border-radius:var(--radius-sm);font-size:0.9rem;margin-bottom:16px;"></div>
            <form id="transferForm">
                <div class="form-group">
                    <label class="form-label">Từ tài khoản</label>
                    <select id="fromAccount" class="form-input form-select" required>
                        <option value="">Chọn tài khoản nguồn</option>
                    </select>
                </div>
                <div class="form-group">
                    <label class="form-label">Đến tài khoản</label>
                    <input type="text" id="toAccount" class="form-input" placeholder="Nhập số tài khoản đích" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Số tiền</label>
                    <input type="number" id="amount" step="1000" min="1000" class="form-input" placeholder="Nhập số tiền (VNĐ)" required>
                </div>
                <div class="form-group">
                    <label class="form-label">Nội dung</label>
                    <input type="text" id="description" class="form-input" placeholder="Nội dung chuyển khoản">
                </div>
                <button type="submit" id="transferBtn" class="btn btn-primary btn-lg btn-full">
                    💸 Chuyển tiền
                </button>
            </form>
            <p style="margin-top:16px;font-size:0.8rem;color:var(--text-muted);">
                * Giao dịch trên <strong>10.000.000₫</strong> cần xác thực gương mặt
            </p>
            <p style="margin-top:8px;text-align:center;"><a href="/dashboard" class="btn btn-outline btn-sm">← Về tổng quan</a></p>
        </div>
    </main>

    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script>
        var currentFaceToken = null;

        fetch('/api/accounts', { credentials: 'include' })
            .then(function(r) { return r.json(); })
            .then(function(accounts) {
                var sel = document.getElementById('fromAccount');
                accounts.forEach(function(a) {
                    var opt = document.createElement('option');
                    opt.value = a.id;
                    opt.textContent = a.accountNumber + ' (' + formatVND(a.balance) + ')';
                    sel.appendChild(opt);
                });
            });

        document.getElementById('transferForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            var btn = document.getElementById('transferBtn');
            var msg = document.getElementById('message');
            btn.disabled = true; btn.textContent = 'Đang xử lý...';
            msg.style.display = 'none';

            var amount = parseFloat(document.getElementById('amount').value);
            var body = {
                fromAccountId: parseInt(document.getElementById('fromAccount').value),
                toAccountNumber: document.getElementById('toAccount').value,
                amount: amount,
                description: document.getElementById('description').value
            };

            // If amount > 10M, require face verification first
            if (amount > 10000000) {
                if (!currentFaceToken) {
                    msg.style.display = 'block';
                    msg.style.background = '#fef3c7'; msg.style.color = '#92400e';
                    msg.textContent = 'Giao dịch trên 10.000.000₫ cần xác thực gương mặt. Đang mở camera...';
                    await new Promise(function(resolve) {
                        openCamera(async function(base64) {
                            msg.textContent = 'Đang xác thực...';
                            var faceRes = await apiFetch('/api/face/verify-for-transfer', {
                                method: 'POST',
                                body: { faceImage: base64 }
                            });
                            if (faceRes.ok && faceRes.data.faceToken) {
                                currentFaceToken = faceRes.data.faceToken;
                                showToast('Xác thực gương mặt thành công', 'success');
                                resolve();
                            } else {
                                msg.style.background = '#fce4ec'; msg.style.color = 'var(--danger)';
                                msg.textContent = faceRes.data.error || 'Xác thực thất bại';
                                msg.style.display = 'block';
                                btn.disabled = false; btn.textContent = '💸 Chuyển tiền';
                                resolve(); // won't proceed due to currentFaceToken being null
                            }
                        });
                    });
                    if (!currentFaceToken) {
                        btn.disabled = false; btn.textContent = '💸 Chuyển tiền';
                        return; // Face verification failed, stop
                    }
                }
                body.faceToken = currentFaceToken;
            }

            var res = await apiFetch('/api/transactions/transfer', {
                method: 'POST',
                body: body
            });

            if (res.ok) {
                msg.style.display = 'block';
                msg.style.background = '#d1fae5'; msg.style.color = '#065f46';
                msg.textContent = '✅ Chuyển tiền thành công! Mã GD: ' + res.data.transactionCode;
                document.getElementById('transferForm').reset();
                currentFaceToken = null;
                // Refresh account list
                fetch('/api/accounts', { credentials: 'include' })
                    .then(function(r) { return r.json(); })
                    .then(function(accounts) {
                        var sel = document.getElementById('fromAccount');
                        sel.innerHTML = '<option value="">Chọn tài khoản nguồn</option>';
                        accounts.forEach(function(a) {
                            var opt = document.createElement('option');
                            opt.value = a.id;
                            opt.textContent = a.accountNumber + ' (' + formatVND(a.balance) + ')';
                            sel.appendChild(opt);
                        });
                    });
            } else {
                msg.style.display = 'block';
                msg.style.background = '#fce4ec'; msg.style.color = 'var(--danger)';
                msg.textContent = '❌ ' + (res.data.error || 'Giao dịch thất bại');
            }
            btn.disabled = false; btn.textContent = '💸 Chuyển tiền';
        });
    </script>
</body>
</html>
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/transfer.html
git commit -m "feat: redesign transfer page with face verification for >10M"
```

---

### Task 11: Frontend — Remaining pages (register, history, beneficiaries, scheduled, profile)

**Files:**
- Modify: `src/main/resources/templates/register.html`
- Modify: `src/main/resources/templates/history.html`
- Modify: `src/main/resources/templates/beneficiaries.html`
- Modify: `src/main/resources/templates/scheduled.html`
- Modify: `src/main/resources/templates/profile.html`

- [ ] **Step 1: Rewrite register.html** (standalone login/register style, no layout)

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Đăng ký')"></head>
<body style="background:linear-gradient(135deg, var(--primary-dark), var(--primary));min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;">
    <div style="background:var(--card);border-radius:var(--radius);box-shadow:var(--shadow-lg);padding:40px;max-width:420px;width:100%;">
        <div style="text-align:center;margin-bottom:32px;">
            <div style="font-size:3rem;margin-bottom:8px;">🚀</div>
            <h1 style="font-size:1.5rem;font-weight:700;color:var(--text);">Tạo tài khoản</h1>
            <p style="color:var(--text-muted);font-size:0.9rem;margin-top:4px;">Tham gia Ngân Hàng ngay hôm nay</p>
        </div>
        <div id="errorMsg" style="display:none;background:#fce4ec;color:var(--danger);padding:12px 16px;border-radius:var(--radius-sm);font-size:0.85rem;margin-bottom:16px;"></div>
        <div id="successMsg" style="display:none;background:#d1fae5;color:#065f46;padding:12px 16px;border-radius:var(--radius-sm);font-size:0.85rem;margin-bottom:16px;"></div>
        <form id="registerForm">
            <div class="form-group">
                <label class="form-label">Họ và tên</label>
                <input type="text" id="fullName" class="form-input" placeholder="Nguyễn Văn A" required>
            </div>
            <div class="form-group">
                <label class="form-label">Tên đăng nhập</label>
                <input type="text" id="username" class="form-input" placeholder="username" required>
            </div>
            <div class="form-group">
                <label class="form-label">Email</label>
                <input type="email" id="email" class="form-input" placeholder="email@example.com" required>
            </div>
            <div class="form-group">
                <label class="form-label">Mật khẩu</label>
                <input type="password" id="password" class="form-input" placeholder="••••••••" required>
            </div>
            <button type="submit" id="registerBtn" class="btn btn-primary btn-lg btn-full">Tạo tài khoản</button>
        </form>
        <p style="text-align:center;margin-top:24px;font-size:0.85rem;color:var(--text-muted);">
            Đã có tài khoản? <a href="/login" style="color:var(--primary);font-weight:600;text-decoration:none;">Đăng nhập</a>
        </p>
    </div>
    <script th:src="@{/js/banking.js}"></script>
    <script>
        document.getElementById('registerForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            var btn = document.getElementById('registerBtn');
            var err = document.getElementById('errorMsg');
            var success = document.getElementById('successMsg');
            btn.disabled = true; btn.textContent = 'Đang tạo...';
            err.style.display = 'none'; success.style.display = 'none';
            var res = await apiFetch('/api/auth/register', {
                method: 'POST',
                body: {
                    fullName: document.getElementById('fullName').value,
                    username: document.getElementById('username').value,
                    email: document.getElementById('email').value,
                    password: document.getElementById('password').value
                }
            });
            if (res.ok) {
                success.textContent = 'Đăng ký thành công! Đang chuyển đến trang đăng nhập...';
                success.style.display = 'block';
                await apiFetch('/api/auth/logout', { method: 'POST' });
                setTimeout(function() { window.location.href = '/login'; }, 1000);
            } else {
                err.textContent = res.data.error || 'Đăng ký thất bại';
                err.style.display = 'block';
            }
            btn.disabled = false; btn.textContent = 'Tạo tài khoản';
        });
    </script>
</body>
</html>
```

- [ ] **Step 2: Rewrite history.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Lịch sử')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('history')"></nav>
    <aside th:replace="fragments/layout :: sidebar('history')"></aside>
    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">📋 Lịch sử giao dịch</h1>
            <p class="page-subtitle">Xem tất cả giao dịch đã thực hiện</p>
        </div>
        <div class="card">
            <div class="table-container">
                <table class="table">
                    <thead><tr><th>Mã GD</th><th>Số tiền</th><th>Loại</th><th>Nội dung</th><th>Ngày</th></tr></thead>
                    <tbody id="txBody"></tbody>
                </table>
            </div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script>
        fetch('/api/transactions/history?page=0&size=50', { credentials: 'include' })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var body = document.getElementById('txBody');
                if (data.content && data.content.length) {
                    data.content.forEach(function(tx) {
                        var row = body.insertRow();
                        row.innerHTML = '<td style="font-family:monospace;font-size:0.85rem;">' + tx.transactionCode + '</td>'
                            + '<td style="font-weight:600;">' + formatVND(tx.amount) + '</td>'
                            + '<td><span class="badge badge-info">' + tx.type + '</span></td>'
                            + '<td>' + (tx.description || '') + '</td>'
                            + '<td>' + formatDate(tx.createdAt) + '</td>';
                    });
                } else {
                    body.innerHTML = '<tr><td colspan="5" class="empty">Chưa có giao dịch nào</td></tr>';
                }
            })
            .catch(function() { showToast('Không thể tải lịch sử', 'error'); });
    </script>
</body>
</html>
```

- [ ] **Step 3: Rewrite beneficiaries.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Thụ hưởng')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('beneficiaries')"></nav>
    <aside th:replace="fragments/layout :: sidebar('beneficiaries')"></aside>
    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">👥 Người thụ hưởng</h1>
            <p class="page-subtitle">Quản lý danh sách người nhận thường xuyên</p>
        </div>
        <div class="card" style="margin-bottom:24px;">
            <div class="card-header"><div class="card-title">Thêm người thụ hưởng</div></div>
            <form id="addForm" style="display:flex;gap:12px;flex-wrap:wrap;">
                <input type="text" id="accountNumber" class="form-input" placeholder="Số tài khoản" required style="flex:2;min-width:200px;">
                <input type="text" id="nickname" class="form-input" placeholder="Tên gợi nhớ" style="flex:1;min-width:140px;">
                <button type="submit" class="btn btn-primary">➕ Thêm</button>
            </form>
        </div>
        <div class="card">
            <div class="table-container">
                <table class="table">
                    <thead><tr><th>Số tài khoản</th><th>Tên gợi nhớ</th><th>Thao tác</th></tr></thead>
                    <tbody id="beneficiariesBody"></tbody>
                </table>
            </div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script>
        function loadBeneficiaries() {
            fetch('/api/users/beneficiaries', { credentials: 'include' })
                .then(function(r) { return r.json(); })
                .then(function(data) {
                    var body = document.getElementById('beneficiariesBody');
                    body.innerHTML = '';
                    if (data.length) {
                        data.forEach(function(b) {
                            var row = body.insertRow();
                            row.innerHTML = '<td style="font-family:monospace;">' + b.accountNumber + '</td>'
                                + '<td>' + (b.nickname || '') + '</td>'
                                + '<td><button onclick="deleteBeneficiary(' + b.id + ')" class="btn btn-danger btn-sm">Xóa</button></td>';
                        });
                    } else {
                        body.innerHTML = '<tr><td colspan="3" class="empty">Chưa có người thụ hưởng</td></tr>';
                    }
                });
        }
        function deleteBeneficiary(id) {
            fetch('/api/users/beneficiaries/' + id, { method: 'DELETE', credentials: 'include' })
                .then(function() { loadBeneficiaries(); showToast('Đã xóa', 'success'); });
        }
        document.getElementById('addForm').addEventListener('submit', function(e) {
            e.preventDefault();
            fetch('/api/users/beneficiaries', {
                method: 'POST', headers: { 'Content-Type': 'application/json' }, credentials: 'include',
                body: JSON.stringify({ accountNumber: document.getElementById('accountNumber').value, nickname: document.getElementById('nickname').value })
            }).then(function() {
                document.getElementById('accountNumber').value = '';
                document.getElementById('nickname').value = '';
                loadBeneficiaries();
                showToast('Đã thêm người thụ hưởng', 'success');
            });
        });
        loadBeneficiaries();
    </script>
</body>
</html>
```

- [ ] **Step 4: Rewrite scheduled.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Định kỳ')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('scheduled')"></nav>
    <aside th:replace="fragments/layout :: sidebar('scheduled')"></aside>
    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">⏰ Thanh toán định kỳ</h1>
            <p class="page-subtitle">Quản lý các giao dịch tự động</p>
        </div>
        <div class="card">
            <div class="table-container">
                <table class="table">
                    <thead><tr><th>Đến tài khoản</th><th>Số tiền</th><th>Tần suất</th><th>Lần chạy tiếp</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                    <tbody id="paymentsBody"></tbody>
                </table>
            </div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script>
        fetch('/api/scheduled-payments', { credentials: 'include' })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                var body = document.getElementById('paymentsBody');
                if (data.length) {
                    data.forEach(function(p) {
                        var row = body.insertRow();
                        row.innerHTML = '<td style="font-family:monospace;">' + p.toAccountNumber + '</td>'
                            + '<td style="font-weight:600;">' + formatVND(p.amount) + '</td>'
                            + '<td>' + p.frequency + '</td>'
                            + '<td>' + p.nextRun + '</td>'
                            + '<td><span class="badge ' + (p.status === 'ACTIVE' ? 'badge-success' : 'badge-gray') + '">' + p.status + '</span></td>'
                            + '<td>' + (p.status === 'ACTIVE' ? '<button onclick="cancel(' + p.id + ')" class="btn btn-danger btn-sm">Hủy</button>' : '') + '</td>';
                    });
                } else {
                    body.innerHTML = '<tr><td colspan="6" class="empty">Chưa có thanh toán định kỳ</td></tr>';
                }
            });
        function cancel(id) {
            fetch('/api/scheduled-payments/' + id, { method: 'DELETE', credentials: 'include' })
                .then(function() { location.reload(); });
        }
    </script>
</body>
</html>
```

- [ ] **Step 5: Rewrite profile.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Hồ sơ')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('profile')"></nav>
    <aside th:replace="fragments/layout :: sidebar('profile')"></aside>
    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">👤 Hồ sơ cá nhân</h1>
            <p class="page-subtitle">Quản lý thông tin và bảo mật gương mặt</p>
        </div>
        <div style="display:grid;grid-template-columns:1fr 1fr;gap:24px;">
            <div class="card">
                <div class="card-header"><div class="card-title">Thông tin tài khoản</div></div>
                <div id="profileData"></div>
            </div>
            <div class="card">
                <div class="card-header"><div class="card-title">🔐 Bảo mật gương mặt</div></div>
                <p style="font-size:0.9rem;color:var(--text-muted);margin-bottom:16px;">
                    Đăng ký gương mặt để sử dụng đăng nhập nhanh và xác thực giao dịch trên 10.000.000₫.
                </p>
                <div id="faceStatus" style="margin-bottom:16px;font-size:0.9rem;"></div>
                <button id="registerFaceBtn" class="btn btn-primary btn-lg btn-full" onclick="registerFace()">
                    📸 Đăng ký gương mặt
                </button>
            </div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script>
        fetch('/api/users/profile', { credentials: 'include' })
            .then(function(r) { return r.json(); })
            .then(function(u) {
                document.getElementById('profileData').innerHTML =
                    '<div style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Tên đăng nhập</strong><br>' + u.username + '</div>'
                    + '<div style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Họ và tên</strong><br>' + (u.fullName || '') + '</div>'
                    + '<div style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Email</strong><br>' + (u.email || '') + '</div>'
                    + '<div style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Số điện thoại</strong><br>' + (u.phone || 'Chưa cập nhật') + '</div>'
                    + '<div><strong style="color:var(--text-muted);font-size:0.85rem;">Hạng</strong><br><span class="badge badge-info">' + (u.tier || 'STANDARD') + '</span></div>';
                var faceEnabled = u.faceEnabled;
                document.getElementById('faceStatus').innerHTML = faceEnabled
                    ? '<span class="badge badge-success">✓ Đã đăng ký gương mặt</span>'
                    : '<span class="badge badge-gray">✗ Chưa đăng ký</span>';
                document.getElementById('registerFaceBtn').textContent = faceEnabled ? '📸 Cập nhật gương mặt' : '📸 Đăng ký gương mặt';
            });

        function registerFace() {
            openCamera(async function(base64) {
                var btn = document.getElementById('registerFaceBtn');
                btn.disabled = true; btn.textContent = 'Đang xử lý...';
                var res = await apiFetch('/api/face/register', {
                    method: 'POST',
                    headers: {},
                    body: (function() {
                        var fd = new FormData();
                        var blob = new Blob([Uint8Array.from(atob(base64), function(c) { return c.charCodeAt(0); })], { type: 'image/jpeg' });
                        fd.append('file', blob, 'face.jpg');
                        return fd;
                    })()
                });
                if (res.ok) {
                    showToast('Đăng ký gương mặt thành công!', 'success');
                    document.getElementById('faceStatus').innerHTML = '<span class="badge badge-success">✓ Đã đăng ký gương mặt</span>';
                    btn.textContent = '📸 Cập nhật gương mặt';
                } else {
                    showToast(res.data.error || 'Đăng ký thất bại', 'error');
                }
                btn.disabled = false;
            });
        }
    </script>
</body>
</html>
```

- [ ] **Step 6: Commit all pages**

```bash
git add src/main/resources/templates/register.html src/main/resources/templates/history.html src/main/resources/templates/beneficiaries.html src/main/resources/templates/scheduled.html src/main/resources/templates/profile.html
git commit -m "feat: redesign remaining pages with layout and VND"
```

---

### Task 12: Frontend — Admin pages

**Files:**
- Modify: `src/main/resources/templates/admin/users.html`
- Modify: `src/main/resources/templates/admin/transactions.html`

- [ ] **Step 1: Rewrite admin/users.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="fragments/layout :: head('Quản trị')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('admin')"></nav>
    <aside th:replace="fragments/layout :: sidebar('admin')"></aside>
    <main class="main-content">
        <div class="page-header" style="display:flex;justify-content:space-between;align-items:center;">
            <div>
                <h1 class="page-title">👥 Quản lý người dùng</h1>
                <p class="page-subtitle">Xem và quản lý tất cả người dùng hệ thống</p>
            </div>
            <span class="badge badge-info" style="font-size:1rem;padding:8px 16px;" id="userCount">0 người dùng</span>
        </div>
        <div class="card">
            <div class="table-container">
                <table class="table">
                    <thead><tr><th>ID</th><th>Username</th><th>Họ tên</th><th>Email</th><th>Vai trò</th><th>Trạng thái</th><th>Thao tác</th></tr></thead>
                    <tbody id="usersBody"></tbody>
                </table>
            </div>
            <div id="loadingState" style="text-align:center;padding:40px;color:var(--text-muted);">Đang tải...</div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script>
        fetch('/api/admin/users', { credentials: 'include' })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                document.getElementById('loadingState').style.display = 'none';
                var body = document.getElementById('usersBody');
                var users = data.content || [];
                document.getElementById('userCount').textContent = users.length + ' người dùng';
                if (users.length) {
                    users.forEach(function(u) {
                        var row = body.insertRow();
                        var roleBadge = u.role === 'ADMIN'
                            ? '<span class="badge badge-purple">ADMIN</span>'
                            : '<span class="badge badge-gray">USER</span>';
                        var statusBadge = u.status === 'LOCKED'
                            ? '<span class="badge badge-danger">KHÓA</span>'
                            : '<span class="badge badge-success">HOẠT ĐỘNG</span>';
                        var actionBtn = u.status !== 'LOCKED'
                            ? '<button onclick="lockUser(' + u.id + ')" class="btn btn-danger btn-sm">Khóa</button>'
                            : '<button onclick="unlockUser(' + u.id + ')" class="btn btn-success btn-sm">Mở khóa</button>';
                        row.innerHTML = '<td style="color:var(--text-muted);font-size:0.85rem;">' + u.id + '</td>'
                            + '<td style="font-weight:600;">' + u.username + '</td>'
                            + '<td>' + u.fullName + '</td>'
                            + '<td style="color:var(--text-muted);">' + u.email + '</td>'
                            + '<td>' + roleBadge + '</td>'
                            + '<td>' + statusBadge + '</td>'
                            + '<td>' + actionBtn + '</td>';
                    });
                } else {
                    body.innerHTML = '<tr><td colspan="7" class="empty">Không có người dùng</td></tr>';
                }
            })
            .catch(function() { document.getElementById('loadingState').textContent = 'Không thể tải dữ liệu.'; });

        function lockUser(id) { if (confirm('Khóa người dùng này?')) { fetch('/api/admin/users/' + id + '/lock', { method: 'POST', credentials: 'include' }).then(function(r) { if (r.ok) location.reload(); }); } }
        function unlockUser(id) { if (confirm('Mở khóa người dùng này?')) { fetch('/api/admin/users/' + id + '/unlock', { method: 'POST', credentials: 'include' }).then(function(r) { if (r.ok) location.reload(); }); } }
    </script>
</body>
</html>
```

- [ ] **Step 2: Rewrite admin/transactions.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" xmlns:sec="http://www.thymeleaf.org/extras/spring-security">
<head th:replace="fragments/layout :: head('Quản trị')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('admin')"></nav>
    <aside th:replace="fragments/layout :: sidebar('admin')"></aside>
    <main class="main-content">
        <div class="page-header" style="display:flex;justify-content:space-between;align-items:center;">
            <div>
                <h1 class="page-title">📊 Tất cả giao dịch</h1>
                <p class="page-subtitle">Xem tất cả giao dịch trong hệ thống</p>
            </div>
            <span class="badge badge-info" style="font-size:1rem;padding:8px 16px;" id="txCount">Đang tải...</span>
        </div>
        <div class="card">
            <div class="table-container">
                <table class="table">
                    <thead><tr><th>Mã GD</th><th>Từ tài khoản</th><th>Đến tài khoản</th><th>Số tiền</th><th>Loại</th><th>Trạng thái</th><th>Ngày</th></tr></thead>
                    <tbody id="txBody"></tbody>
                </table>
            </div>
            <div id="loadingState" style="text-align:center;padding:40px;color:var(--text-muted);">Đang tải...</div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <script>
        fetch('/api/admin/transactions?page=0&size=100', { credentials: 'include' })
            .then(function(r) { return r.json(); })
            .then(function(data) {
                document.getElementById('loadingState').style.display = 'none';
                var body = document.getElementById('txBody');
                var txs = data.content || data;
                document.getElementById('txCount').textContent = txs.length + ' giao dịch';
                if (txs.length) {
                    txs.forEach(function(tx) {
                        var row = body.insertRow();
                        var typeBadge = tx.type === 'TRANSFER'
                            ? '<span class="badge badge-info">↔ Chuyển</span>'
                            : tx.type === 'DEPOSIT'
                            ? '<span class="badge badge-success">↓ Nạp</span>'
                            : '<span class="badge badge-warning">↑ Rút</span>';
                        var statusBadge = tx.status === 'COMPLETED'
                            ? '<span class="badge badge-success">HOÀN TẤT</span>'
                            : '<span class="badge badge-warning">CHỜ</span>';
                        row.innerHTML = '<td style="font-family:monospace;font-size:0.85rem;">' + tx.transactionCode + '</td>'
                            + '<td style="font-size:0.85rem;">' + (tx.fromAccountNumber || '-') + '</td>'
                            + '<td style="font-size:0.85rem;">' + (tx.toAccountNumber || '-') + '</td>'
                            + '<td style="font-weight:600;">' + formatVND(tx.amount) + '</td>'
                            + '<td>' + typeBadge + '</td>'
                            + '<td>' + statusBadge + '</td>'
                            + '<td style="font-size:0.85rem;color:var(--text-muted);">' + formatDateShort(tx.createdAt) + '</td>';
                    });
                } else {
                    body.innerHTML = '<tr><td colspan="7" class="empty">Không có giao dịch</td></tr>';
                }
            })
            .catch(function() { document.getElementById('loadingState').textContent = 'Không thể tải dữ liệu.'; });
    </script>
</body>
</html>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/admin/users.html src/main/resources/templates/admin/transactions.html
git commit -m "feat: redesign admin pages with layout and VND"
```

---

### Task 13: Compile & verify

**Files:** none

- [ ] **Step 1: Compile full project**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run tests**

Run: `mvn test`
Expected: 7/7 tests pass

- [ ] **Step 3: Final commit (if any fixes needed)**

```bash
git add -A
git commit -m "chore: final fixes after banking UI redesign"
```
