# Settings Page & Quick Face Login Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace `/profile` with a full banking-style `/settings` page (6 tabbed sections) and add quick face login (no username required) via LBPH identification.

**Architecture:** Single settings page at `/settings` with JS tab switching. New `POST /api/auth/face-quick-login` endpoint identifies user by face only. New `identifyFace()` method on `JavaCVFaceEngine` uses LBPH predict to find userId without username.

**Tech Stack:** Spring Boot 3.2.4, Java 21, Thymeleaf, Tailwind CSS, JavaCV 1.5.10

---

### Task 1: User entity — add emailNotifications, language fields

**Files:**
- Modify: `src/main/java/com/moneytransfer/user/User.java`

- [ ] Add fields after `createdAt` (line 55):

```java
@Column(nullable = false)
private boolean emailNotifications = true;

@Column(length = 5)
private String language = "vi";
```

- [ ] Add getters/setters before closing brace:

```java
public boolean isEmailNotifications() { return emailNotifications; }
public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
public String getLanguage() { return language; }
public void setLanguage(String language) { this.language = language; }
```

---

### Task 2: JavaCVFaceEngine — add identifyFace()

**Files:**
- Modify: `src/main/java/com/moneytransfer/face/JavaCVFaceEngine.java`

- [ ] Add import after line 6:

```java
import java.util.Optional;
```

- [ ] Add method after `verifyFace` (after line 56):

```java
public Optional<Long> identifyFace(byte[] imageBytes) {
    if (imageBytes == null || imageBytes.length == 0) {
        throw new IllegalArgumentException("imageBytes must not be null or empty");
    }
    if (!faceRecognizer.isTrained()) {
        log.warn("LBPH model not trained yet");
        return Optional.empty();
    }
    Mat source = decodeImage(imageBytes);
    Mat faceRoi = faceDetector.cropLargestFace(source);
    Mat processed = preprocessor.preprocess(faceRoi);
    FaceRecognizer.RecognizerResult result = faceRecognizer.predict(processed);
    if (result.confidence() < 80.0) {
        log.info("Face identify: predictedLabel={}, confidence={}", result.label(), result.confidence());
        return Optional.of((long) result.label());
    }
    log.warn("Face identify: no match, confidence={}", result.confidence());
    return Optional.empty();
}
```

---

### Task 3: FaceService — add identifyFaceBase64Sync()

**Files:**
- Modify: `src/main/java/com/moneytransfer/face/FaceService.java`

- [ ] Add import after line 8:

```java
import java.util.Optional;
```

- [ ] Add method after `verifyFaceBase64Sync` (after line 65):

```java
public User identifyFaceBase64Sync(String base64Image) {
    byte[] imageBytes = Base64.getDecoder().decode(base64Image);
    Optional<Long> userIdOpt = javaCVFaceEngine.identifyFace(imageBytes);
    Long userId = userIdOpt.orElseThrow(() -> new RuntimeException("Không nhận diện được khuôn mặt"));
    return userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
}
```

---

### Task 4: AuthController — add changePassword() and quickFaceLogin()

**Files:**
- Modify: `src/main/java/com/moneytransfer/auth/AuthController.java`

- [ ] Add after constructor (before first endpoint) — inject PasswordEncoder:

```java
import org.springframework.security.crypto.password.PasswordEncoder;
```

Add field:
```java
private final PasswordEncoder passwordEncoder;
```

Update constructor to accept `PasswordEncoder passwordEncoder`.

- [ ] Add after `faceLogin()` method (after line 122):

```java
@PostMapping("/change-password")
public ResponseEntity<?> changePassword(Authentication auth, @RequestBody Map<String, String> body,
                                         HttpServletResponse response) {
    try {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        String oldPassword = body.get("oldPassword");
        String newPassword = body.get("newPassword");
        if (oldPassword == null || newPassword == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Old password and new password are required"));
        }
        if (newPassword.length() < 6) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu mới phải có ít nhất 6 ký tự"));
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Mật khẩu cũ không đúng"));
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}

@PostMapping("/face-quick-login")
public ResponseEntity<?> quickFaceLogin(@RequestBody Map<String, String> body,
                                         HttpServletRequest httpRequest,
                                         HttpServletResponse response) {
    try {
        String faceImage = body.get("faceImage");
        if (faceImage == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "faceImage is required"));
        }
        User user = faceService.identifyFaceBase64Sync(faceImage);
        if (user.isLocked()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tài khoản đã bị khóa"));
        }
        if (!user.isFaceEnabled()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Chưa đăng ký gương mặt"));
        }
        user.setFailedAttempts(0);
        userRepository.save(user);

        String ip = httpRequest.getRemoteAddr();
        auditLogRepository.save(new AuditLog(user.getId(), "FACE_LOGIN", "Quick face login successful", ip));

        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole().name());
        authService.setAuthCookies(response, accessToken, refreshToken);

        return ResponseEntity.ok(Map.of("message", "Đăng nhập thành công", "userId", user.getId()));
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
```

- [ ] Update the `AuthenticationManager` import — add `PasswordEncoder` import:

```java
import org.springframework.security.crypto.password.PasswordEncoder;
```

- [ ] Also update the constructor to add `PasswordEncoder` parameter between `auditLogRepository` and closing paren:

```java
public AuthController(AuthService authService, JwtUtil jwtUtil,
                      FaceService faceService, UserRepository userRepository,
                      AuditLogRepository auditLogRepository,
                      PasswordEncoder passwordEncoder) {
    this.authService = authService;
    this.jwtUtil = jwtUtil;
    this.faceService = faceService;
    this.userRepository = userRepository;
    this.auditLogRepository = auditLogRepository;
    this.passwordEncoder = passwordEncoder;
}
```

---

### Task 5: UserService — add emailNotifications, language to updateProfile()

**Files:**
- Modify: `src/main/java/com/moneytransfer/user/UserService.java`

- [ ] Add after line 33 (`faceEnabled` update):

```java
if (updates.containsKey("emailNotifications")) user.setEmailNotifications(Boolean.parseBoolean(updates.get("emailNotifications")));
if (updates.containsKey("language")) user.setLanguage(updates.get("language"));
```

---

### Task 6: UserController — add new fields to getProfile()

**Files:**
- Modify: `src/main/java/com/moneytransfer/user/UserController.java`

- [ ] Add to the map in `getProfile()` after `faceEnabled` (line 33):

```java
"emailNotifications", user.isEmailNotifications(),
"language", user.getLanguage() != null ? user.getLanguage() : "vi"
```

---

### Task 7: WebController — add /settings, remove /profile

**Files:**
- Modify: `src/main/java/com/moneytransfer/WebController.java`

- [ ] Change line 15:
```
Old: @GetMapping("/profile") public String profile() { return "profile"; }
New: @GetMapping("/profile") public String profile() { return "redirect:/settings"; }
New: @GetMapping("/settings") public String settings() { return "settings"; }
```

Final code for the two methods:
```java
@GetMapping("/profile") public String profile() { return "redirect:/settings"; }
@GetMapping("/settings") public String settings() { return "settings"; }
```

---

### Task 8: Layout — rename "Hồ sơ" → "Cài đặt", update sidebar and nav

**Files:**
- Modify: `src/main/resources/templates/fragments/layout.html`

- [ ] Change navbar profile link (line 21):
```
Old: <a th:href="@{/profile}" class="nav-link" title="Hồ sơ">👤 <span th:text="${#authentication.name}">user</span></a>
New: <a th:href="@{/settings}" class="nav-link" title="Cài đặt">⚙️ <span th:text="${#authentication.name}">user</span></a>
```

- [ ] Change sidebar profile link (line 44-46):
```
Old: <a th:href="@{/profile}" th:classappend="${currentPage == 'profile'} ? 'active'" class="sidebar-item">
         <span class="icon">👤</span> Hồ sơ
     </a>
New: <a th:href="@{/settings}" th:classappend="${currentPage == 'settings'} ? 'active'" class="sidebar-item">
         <span class="icon">⚙️</span> Cài đặt
     </a>
```

---

### Task 9: Login page — add quick face login button

**Files:**
- Modify: `src/main/resources/templates/login.html`

- [ ] Add quick login button after the existing face login button (after line 26):

```html
<button id="quickFaceLoginBtn" class="btn btn-outline btn-lg btn-full" style="margin-top:8px;" onclick="quickFaceLogin()">
    ⚡ Đăng nhập nhanh
</button>
```

- [ ] Add JS function after `openFaceLogin()` (after line 60):

```javascript
async function quickFaceLogin() {
    openCamera(async function(base64) {
        var btn = document.getElementById('quickFaceLoginBtn');
        btn.disabled = true; btn.textContent = 'Đang xác thực...';
        var res = await apiFetch('/api/auth/face-quick-login', {
            method: 'POST',
            body: { faceImage: base64 }
        });
        if (res.ok) { window.location.href = '/dashboard'; }
        else { showToast(res.data.error || 'Đăng nhập thất bại', 'error'); }
        btn.disabled = false; btn.textContent = '⚡ Đăng nhập nhanh';
    });
}
```

---

### Task 10: Settings page — create settings.html with 6 tabbed sections

**Files:**
- Create: `src/main/resources/templates/settings.html`

- [ ] Create file:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Cài đặt')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('settings')"></nav>
    <aside th:replace="fragments/layout :: sidebar('settings')"></aside>
    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">⚙️ Cài đặt</h1>
            <p class="page-subtitle">Quản lý tài khoản và bảo mật</p>
        </div>

        <div class="tabs" style="display:flex;gap:4px;margin-bottom:24px;border-bottom:2px solid var(--border);padding-bottom:0;flex-wrap:wrap;">
            <button class="tab-btn active" data-tab="personal">👤 Cá nhân</button>
            <button class="tab-btn" data-tab="security">🔒 Bảo mật</button>
            <button class="tab-btn" data-tab="facelogin">📸 Đăng nhập nhanh</button>
            <button class="tab-btn" data-tab="limits">💰 Hạn mức</button>
            <button class="tab-btn" data-tab="notifications">🔔 Thông báo</button>
            <button class="tab-btn" data-tab="language">🌐 Ngôn ngữ</button>
        </div>

        <div id="tab-personal" class="tab-content active">
            <div class="card">
                <div class="card-header"><div class="card-title">👤 Thông tin cá nhân</div></div>
                <div class="card-body">
                    <div class="form-group"><label class="form-label">Tên đăng nhập</label><input type="text" id="p-username" class="form-input" disabled></div>
                    <div class="form-group"><label class="form-label">Họ và tên</label><input type="text" id="p-fullName" class="form-input"></div>
                    <div class="form-group"><label class="form-label">Email</label><input type="email" id="p-email" class="form-input"></div>
                    <div class="form-group"><label class="form-label">Số điện thoại</label><input type="tel" id="p-phone" class="form-input"></div>
                    <div class="form-group" style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Hạng thành viên</strong><br><span id="p-tier" class="badge badge-info">STANDARD</span></div>
                    <button onclick="savePersonalInfo()" class="btn btn-primary">Lưu thay đổi</button>
                </div>
            </div>
        </div>

        <div id="tab-security" class="tab-content">
            <div class="card" style="margin-bottom:16px;">
                <div class="card-header"><div class="card-title">🔑 Đổi mật khẩu</div></div>
                <div class="card-body">
                    <div class="form-group"><label class="form-label">Mật khẩu cũ</label><input type="password" id="s-oldPassword" class="form-input" placeholder="••••••••"></div>
                    <div class="form-group"><label class="form-label">Mật khẩu mới</label><input type="password" id="s-newPassword" class="form-input" placeholder="••••••••"></div>
                    <div class="form-group"><label class="form-label">Xác nhận mật khẩu mới</label><input type="password" id="s-confirmPassword" class="form-input" placeholder="••••••••"></div>
                    <button onclick="changePassword()" class="btn btn-primary">Đổi mật khẩu</button>
                </div>
            </div>
            <div class="card">
                <div class="card-header"><div class="card-title">🔐 Xác thực OTP</div></div>
                <div class="card-body">
                    <label style="display:flex;align-items:center;gap:12px;cursor:pointer;">
                        <input type="checkbox" id="s-otpEnabled" onchange="toggleOtp()"> Bật xác thực OTP khi đăng nhập và giao dịch
                    </label>
                </div>
            </div>
        </div>

        <div id="tab-facelogin" class="tab-content">
            <div class="card">
                <div class="card-header"><div class="card-title">📸 Đăng nhập nhanh bằng gương mặt</div></div>
                <div class="card-body">
                    <p style="font-size:0.9rem;color:var(--text-muted);margin-bottom:16px;">Đăng ký gương mặt để đăng nhập nhanh không cần mật khẩu và xác thực giao dịch trên 10.000.000₫.</p>
                    <div id="f-faceStatus" style="margin-bottom:16px;font-size:0.9rem;"></div>
                    <button id="f-registerFaceBtn" class="btn btn-primary btn-lg btn-full" onclick="registerFace()">📸 Đăng ký gương mặt</button>
                    <div style="margin-top:16px;">
                        <label style="display:flex;align-items:center;gap:12px;cursor:pointer;">
                            <input type="checkbox" id="f-faceEnabled" onchange="toggleFace()"> Cho phép đăng nhập nhanh bằng gương mặt
                        </label>
                    </div>
                </div>
            </div>
        </div>

        <div id="tab-limits" class="tab-content">
            <div class="card">
                <div class="card-header"><div class="card-title">💰 Hạn mức giao dịch</div></div>
                <div class="card-body">
                    <div style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Hạng thành viên</strong><br><span id="l-tier" class="badge badge-info">STANDARD</span></div>
                    <div style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Ngưỡng xác thực gương mặt</strong><br>Giao dịch từ 10.000.000₫ trở lên yêu cầu xác thực gương mặt</div>
                    <div><strong style="color:var(--text-muted);font-size:0.85rem;">Trạng thái</strong><br><span id="l-faceStatus">Chưa đăng ký gương mặt</span></div>
                </div>
            </div>
        </div>

        <div id="tab-notifications" class="tab-content">
            <div class="card">
                <div class="card-header"><div class="card-title">🔔 Thông báo</div></div>
                <div class="card-body">
                    <label style="display:flex;align-items:center;gap:12px;cursor:pointer;">
                        <input type="checkbox" id="n-emailNotifications" onchange="saveNotifications()"> Nhận thông báo giao dịch qua email
                    </label>
                </div>
            </div>
        </div>

        <div id="tab-language" class="tab-content">
            <div class="card">
                <div class="card-header"><div class="card-title">🌐 Ngôn ngữ</div></div>
                <div class="card-body">
                    <div class="form-group">
                        <label class="form-label">Ngôn ngữ hiển thị</label>
                        <select id="l-language" class="form-input" onchange="saveLanguage()">
                            <option value="vi">Tiếng Việt</option>
                            <option value="en">English</option>
                        </select>
                    </div>
                </div>
            </div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <style>
        .tab-btn { padding:10px 20px;border:none;background:transparent;color:var(--text-muted);cursor:pointer;font-size:0.9rem;font-weight:500;border-radius:var(--radius-sm) var(--radius-sm) 0 0;border-bottom:2px solid transparent;margin-bottom:-2px;transition:all 0.2s;}
        .tab-btn:hover { color:var(--text);background:var(--bg);}
        .tab-btn.active { color:var(--primary);border-bottom-color:var(--primary);background:transparent;}
        .tab-content { display:none;}
        .tab-content.active { display:block;}
    </style>
    <script>
        var userData = null;

        document.querySelectorAll('.tab-btn').forEach(function(btn) {
            btn.addEventListener('click', function() {
                document.querySelectorAll('.tab-btn').forEach(function(b) { b.classList.remove('active'); });
                document.querySelectorAll('.tab-content').forEach(function(t) { t.classList.remove('active'); });
                btn.classList.add('active');
                document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
            });
        });

        fetch('/api/users/profile', { credentials: 'include' }).then(function(r) { return r.json(); }).then(function(u) {
            userData = u;
            document.getElementById('p-username').value = u.username;
            document.getElementById('p-fullName').value = u.fullName || '';
            document.getElementById('p-email').value = u.email || '';
            document.getElementById('p-phone').value = u.phone || '';
            document.getElementById('p-tier').textContent = u.tier || 'STANDARD';
            document.getElementById('l-tier').textContent = u.tier || 'STANDARD';
            document.getElementById('s-otpEnabled').checked = u.otpEnabled;
            document.getElementById('n-emailNotifications').checked = u.emailNotifications !== false;
            document.getElementById('l-language').value = u.language || 'vi';
            var faceEnabled = u.faceEnabled;
            document.getElementById('f-faceEnabled').checked = faceEnabled;
            document.getElementById('f-faceStatus').innerHTML = faceEnabled ? '<span class="badge badge-success">✓ Đã đăng ký gương mặt</span>' : '<span class="badge badge-gray">✗ Chưa đăng ký</span>';
            document.getElementById('f-registerFaceBtn').textContent = faceEnabled ? '📸 Cập nhật gương mặt' : '📸 Đăng ký gương mặt';
            document.getElementById('l-faceStatus').innerHTML = faceEnabled ? '<span class="badge badge-success">✓ Đã đăng ký</span>' : '<span class="badge badge-gray">✗ Chưa đăng ký</span>';
        });

        function savePersonalInfo() {
            var body = { fullName: document.getElementById('p-fullName').value, email: document.getElementById('p-email').value, phone: document.getElementById('p-phone').value };
            apiFetch('/api/users/profile', { method: 'PUT', body: body }).then(function(res) {
                if (res.ok) { showToast('Cập nhật thông tin thành công', 'success'); } else { showToast(res.data.error || 'Lỗi', 'error'); }
            });
        }

        function changePassword() {
            var oldPw = document.getElementById('s-oldPassword').value;
            var newPw = document.getElementById('s-newPassword').value;
            var confirmPw = document.getElementById('s-confirmPassword').value;
            if (!oldPw || !newPw || !confirmPw) { showToast('Vui lòng điền đầy đủ thông tin', 'error'); return; }
            if (newPw !== confirmPw) { showToast('Mật khẩu mới không khớp', 'error'); return; }
            if (newPw.length < 6) { showToast('Mật khẩu phải có ít nhất 6 ký tự', 'error'); return; }
            apiFetch('/api/auth/change-password', { method: 'POST', body: { oldPassword: oldPw, newPassword: newPw } }).then(function(res) {
                if (res.ok) { showToast('Đổi mật khẩu thành công', 'success'); document.getElementById('s-oldPassword').value = ''; document.getElementById('s-newPassword').value = ''; document.getElementById('s-confirmPassword').value = ''; }
                else { showToast(res.data.error || 'Lỗi', 'error'); }
            });
        }

        function toggleOtp() {
            apiFetch('/api/users/profile', { method: 'PUT', body: { otpEnabled: document.getElementById('s-otpEnabled').checked.toString() } }).then(function(res) {
                if (res.ok) { showToast('Đã cập nhật', 'success'); } else { showToast('Lỗi', 'error'); }
            });
        }

        function toggleFace() {
            var enabled = document.getElementById('f-faceEnabled').checked;
            apiFetch('/api/users/profile', { method: 'PUT', body: { faceEnabled: enabled.toString() } }).then(function(res) {
                if (res.ok) { showToast(enabled ? 'Đã bật đăng nhập nhanh' : 'Đã tắt đăng nhập nhanh', 'success'); } else { showToast('Lỗi', 'error'); }
            });
        }

        function registerFace() {
            openCamera(async function(base64) {
                var btn = document.getElementById('f-registerFaceBtn');
                btn.disabled = true; btn.textContent = 'Đang xử lý...';
                var res = await apiFetch('/api/face/register', { method: 'POST', body: { faceImage: base64 } });
                if (res.ok) {
                    showToast('Đăng ký gương mặt thành công!', 'success');
                    document.getElementById('f-faceStatus').innerHTML = '<span class="badge badge-success">✓ Đã đăng ký gương mặt</span>';
                    document.getElementById('f-registerFaceBtn').textContent = '📸 Cập nhật gương mặt';
                    document.getElementById('l-faceStatus').innerHTML = '<span class="badge badge-success">✓ Đã đăng ký</span>';
                    document.getElementById('f-faceEnabled').checked = true;
                } else { showToast(res.data.error || 'Đăng ký thất bại', 'error'); }
                btn.disabled = false;
            });
        }

        function saveNotifications() {
            apiFetch('/api/users/profile', { method: 'PUT', body: { emailNotifications: document.getElementById('n-emailNotifications').checked.toString() } }).then(function(res) {
                if (res.ok) { showToast('Đã lưu', 'success'); } else { showToast('Lỗi', 'error'); }
            });
        }

        function saveLanguage() {
            apiFetch('/api/users/profile', { method: 'PUT', body: { language: document.getElementById('l-language').value } }).then(function(res) {
                if (res.ok) { showToast('Đã lưu ngôn ngữ', 'success'); } else { showToast('Lỗi', 'error'); }
            });
        }
    </script>
</body>
</html>
```

---

### Task 11: Delete profile.html

**Files:**
- Delete: `src/main/resources/templates/profile.html`

- [ ] Run:
```bash
Remove-Item -LiteralPath "src/main/resources/templates/profile.html"
```

---

### Task 12: Build and run tests

**Files:**
- (none, verification only)

- [ ] Run:
```bash
mvn clean test
```
Expected: BUILD SUCCESS (23+ tests pass)

---

### Task 13: Restart app and verify

**Files:**
- (none, verification only)

- [ ] Kill old process, start new one:
```bash
Get-Process -Id (netstat -ano | Select-String ":8080 " -SimpleMatch | ForEach-Object { $_ -replace '.*\s+(\d+)$', '$1' } | Select -First 1) -ErrorAction SilentlyContinue | Stop-Process -Force
mvn spring-boot:run
```

- [ ] Wait for startup, then verify:
- Navigate to `http://localhost:8080/settings` — should show settings page with 6 tabs
- Navigate to `http://localhost:8080/profile` — should redirect to `/settings`
- Navigate to `http://localhost:8080/login` — should show "Đăng nhập nhanh" button
- Login with password, verify settings page loads user data
- Register face in Settings tab #3
- Logout, test quick login from login page (requires camera)
