# PIN, QR Payment & Recent Transfers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add PIN-based 2FA, QR code payment (demo), and recent transfer accounts quick-select to the banking app.

**Architecture:** PIN backend (BCrypt hash) + PinController for set/verify. QR handled entirely client-side (qrcodejs + jsQR). RecentTransfers stored in dedicated table, recorded after each transfer. Template changes on login, settings, transfer pages.

**Tech Stack:** Java 26, Spring Boot 3.2.4, Spring Security 6, JPA/Hibernate, MySQL, qrcodejs, jsQR

**Spec:** `docs/superpowers/specs/2026-06-05-pin-qr-recent-transfers.md`

---

### Task 1: Add pinHash and pinSet to User entity

**Files:**
- Modify: `src/main/java/com/moneytransfer/user/User.java`

- [ ] **Step 1: Add fields and migrator logic**

Read the User.java file first. Add these fields:

```java
@Column(name = "pin_hash", length = 255, nullable = true)
private String pinHash;

@Column(name = "pin_set", nullable = false)
private Boolean pinSet = false;
```

Add getters and setters for both fields.

- [ ] **Step 2: Create SQL migration for new columns**

Run manually in MySQL:

```sql
ALTER TABLE users ADD COLUMN pin_hash VARCHAR(255) NULL;
ALTER TABLE users ADD COLUMN pin_set BOOLEAN NOT NULL DEFAULT FALSE;
```

If using Spring auto-ddl, it will create the columns automatically on restart.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/user/User.java
git commit -m "feat: add pinHash and pinSet to User entity"
```

---

### Task 2: Create PinService

**Files:**
- Create: `src/main/java/com/moneytransfer/pin/PinService.java`

- [ ] **Step 1: Create PinService**

```java
package com.moneytransfer.pin;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public PinService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void setPin(User user, String pin) {
        if (pin == null || pin.length() < 4 || pin.length() > 6 || !pin.matches("\\d+")) {
            throw new IllegalArgumentException("PIN must be 4-6 digits");
        }
        user.setPinHash(passwordEncoder.encode(pin));
        user.setPinSet(true);
        userRepository.save(user);
    }

    public boolean verifyPin(User user, String pin) {
        if (!user.getPinSet() || user.getPinHash() == null) return false;
        return passwordEncoder.matches(pin, user.getPinHash());
    }

    public void removePin(User user) {
        user.setPinHash(null);
        user.setPinSet(false);
        userRepository.save(user);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/moneytransfer/pin/PinService.java
git commit -m "feat: create PinService with set/verify/remove"
```

---

### Task 3: Create PinController

**Files:**
- Create: `src/main/java/com/moneytransfer/pin/PinController.java`

- [ ] **Step 1: Add `generateToken` overload to JwtUtil**

Read `src/main/java/com/moneytransfer/auth/JwtUtil.java`. Add this method:

```java
public String generateToken(Long userId, String username, String role, long ttlSeconds) {
    return Jwts.builder()
        .subject(username)
        .claim("userId", userId)
        .claim("role", role)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + ttlSeconds * 1000))
        .signWith(getSigningKey())
        .compact();
}
```

- [ ] **Step 2: Create PinController**

```java
package com.moneytransfer.pin;

import com.moneytransfer.auth.JwtUtil;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/pin")
public class PinController {
    private final PinService pinService;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public PinController(PinService pinService, UserRepository userRepository, JwtUtil jwtUtil) {
        this.pinService = pinService;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/set")
    public ResponseEntity<?> setPin(@RequestBody Map<String, String> body, Authentication auth) {
        String pin = body.get("pin");
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        User user = userRepository.findById(userId).orElseThrow();
        try {
            pinService.setPin(user, pin);
            return ResponseEntity.ok(Map.of("message", "PIN set successfully", "pinSet", true));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, String> body, Authentication auth) {
        String pin = body.get("pin");
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        User user = userRepository.findById(userId).orElseThrow();
        if (pinService.verifyPin(user, pin)) {
            String pinToken = jwtUtil.generateToken(userId, user.getUsername(), user.getRole().name(), 300); // 5 min
            return ResponseEntity.ok(Map.of("valid", true, "pinToken", pinToken));
        }
        return ResponseEntity.status(401).body(Map.of("valid", false, "error", "Invalid PIN"));
    }

    @PostMapping("/remove")
    public ResponseEntity<?> removePin(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        User user = userRepository.findById(userId).orElseThrow();
        pinService.removePin(user);
        return ResponseEntity.ok(Map.of("message", "PIN removed", "pinSet", false));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/pin/PinController.java src/main/java/com/moneytransfer/auth/JwtUtil.java
git commit -m "feat: create PinController with set/verify/remove endpoints"
```

---

### Task 4: Update AuthController to return pinRequired

**Files:**
- Modify: `src/main/java/com/moneytransfer/auth/AuthController.java`
- Modify: `src/main/java/com/moneytransfer/auth/AuthService.java`

- [ ] **Step 1: Read AuthController.login() and AuthService**

Read both files to understand the current login flow.

- [ ] **Step 2: Modify AuthService login method to return pinRequired**

Find the login method in AuthService. In the AuthResponse record and the login method, add pinRequired field. Or better: return it in the response body from AuthController.

Actually, the cleanest approach: modify the response in AuthController.login() to include `pinRequired`:

```java
// In AuthController, after login success:
boolean pinRequired = user.getPinSet() != null && user.getPinSet();
return ResponseEntity.ok(Map.of(
    "accessToken", authResponse.accessToken(),
    "refreshToken", authResponse.refreshToken(),
    "userId", authResponse.userId(),
    "username", authResponse.username(),
    "pinRequired", pinRequired
));
```

Read the current AuthController to find the exact spot and adjust accordingly.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/auth/AuthController.java
git commit -m "feat: return pinRequired in login response"
```

---

### Task 5: Create pin-verify page and WebController mapping

**Files:**
- Create: `src/main/resources/templates/pin-verify.html`
- Modify: `src/main/java/com/moneytransfer/WebController.java`

- [ ] **Step 1: Create pin-verify.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Xác thực PIN')"></head>
<body style="background:linear-gradient(135deg, #0f172a, #1e293b);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;">
    <div style="background:var(--card);border-radius:var(--radius);border:1px solid var(--border-light);box-shadow:var(--shadow-lg);padding:40px;max-width:400px;width:100%;">
        <div style="text-align:center;margin-bottom:28px;">
            <div style="font-size:3rem;margin-bottom:8px;">🔐</div>
            <h1 style="font-size:1.4rem;font-weight:700;color:var(--text);">Nhập mã PIN</h1>
            <p style="color:var(--text-muted);font-size:0.9rem;margin-top:4px;">Nhập mã PIN để tiếp tục</p>
        </div>
        <div id="errorMsg" style="display:none;background:rgba(239,68,68,0.15);color:#f87171;padding:12px 16px;border-radius:var(--radius-sm);font-size:0.85rem;margin-bottom:16px;"></div>
        <form id="pinForm">
            <div class="form-group">
                <input type="password" id="pin" class="form-input" placeholder="Nhập mã PIN" maxlength="6" inputmode="numeric" pattern="[0-9]*" autocomplete="off" style="text-align:center;font-size:1.5rem;letter-spacing:8px;" required>
            </div>
            <button type="submit" id="pinBtn" class="btn btn-primary btn-lg btn-full">Xác thực PIN</button>
        </form>
        <p style="text-align:center;margin-top:20px;font-size:0.85rem;color:var(--text-muted);"><a href="/dashboard" style="color:var(--primary);">Bỏ qua</a></p>
    </div>
    <script th:src="@{/js/banking.js}"></script>
    <script>
        document.getElementById('pinForm').addEventListener('submit', async function(e) {
            e.preventDefault();
            var btn = document.getElementById('pinBtn');
            var err = document.getElementById('errorMsg');
            btn.disabled = true; btn.textContent = 'Đang xác thực...';
            err.style.display = 'none';
            var res = await apiFetch('/api/pin/verify', {
                method: 'POST',
                body: { pin: document.getElementById('pin').value }
            });
            if (res.ok && res.data.valid) {
                window.location.href = '/dashboard';
            } else {
                err.textContent = res.data.error || 'Sai mã PIN';
                err.style.display = 'block';
                btn.disabled = false; btn.textContent = 'Xác thực PIN';
            }
        });
    </script>
</body>
</html>
```

- [ ] **Step 2: Add /pin-verify to WebController**

```java
@GetMapping("/pin-verify") public String pinVerify() { return "pin-verify"; }
```

- [ ] **Step 3: Update login.html JS to redirect to /pin-verify when pinRequired**

Read `login.html`. Find the form submit handler. After login success:

```javascript
if (res.ok) {
    if (res.data.pinRequired) { window.location.href = '/pin-verify'; }
    else { window.location.href = '/dashboard'; }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/pin-verify.html src/main/java/com/moneytransfer/WebController.java src/main/resources/templates/login.html
git commit -m "feat: add pin-verify page and login flow"
```

---

### Task 6: Add PIN check to Transfer

**Files:**
- Modify: `src/main/java/com/moneytransfer/transaction/TransactionController.java`
- Modify: `src/main/resources/templates/transfer.html`

- [ ] **Step 1: Read TransactionController.transfer()**

Read the `transfer()` method in TransactionController.java.

- [ ] **Step 2: Add PIN verification before processing**

Before the existing transfer logic (after parsing request body), add:

```java
// PIN check
Long userId = ((Integer) ((Claims) auth.getDetails()).get("userId")).longValue();
User user = userRepository.findById(userId).orElseThrow();
if (user.getPinSet()) {
    String pinToken = request.getHeader("x-pin-token");
    if (pinToken == null || pinToken.isBlank()) {
        return ResponseEntity.badRequest().body(Map.of("error", "PIN verification required", "pinRequired", true));
    }
    try {
        Claims pinClaims = jwtUtil.validateToken(pinToken);
        Long pinUserId = ((Integer) pinClaims.get("userId")).longValue();
        if (!pinUserId.equals(userId)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid PIN token"));
        }
    } catch (Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "PIN token expired or invalid", "pinRequired", true));
    }
}
```

You'll need to inject `UserRepository` into the controller. Add the necessary import and constructor parameter.

- [ ] **Step 3: Update transfer.html to ask for PIN when pinRequired**

In transfer.html, after the `apiFetch('/api/transactions/transfer', ...)` call, check if response has `pinRequired: true`. If so, show a PIN modal, collect PIN, get pinToken from `/api/pin/verify`, then retry the request with `x-pin-token` header.

Add before the submit handler a helper function:

```javascript
async function requirePin() {
    var pin = prompt('Nhập mã PIN giao dịch:');
    if (!pin) return null;
    var res = await apiFetch('/api/pin/verify', { method: 'POST', body: { pin: pin } });
    if (res.ok && res.data.pinToken) return res.data.pinToken;
    showToast('Sai mã PIN', 'error');
    return null;
}
```

And modify the submit handler to handle `pinRequired`:

```javascript
var res = await apiFetch('/api/transactions/transfer', { method: 'POST', body: body });
if (res.data && res.data.pinRequired) {
    var pinToken = await requirePin();
    if (!pinToken) { btn.disabled = false; btn.textContent = '💸 Chuyển tiền'; return; }
    res = await apiFetch('/api/transactions/transfer', {
        method: 'POST', body: body, headers: { 'x-pin-token': pinToken }
    });
}
// existing success/error handling
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/moneytransfer/transaction/TransactionController.java src/main/resources/templates/transfer.html
git commit -m "feat: add PIN check before transfer"
```

---

### Task 7: Create RecentTransfer entity and repository

**Files:**
- Create: `src/main/java/com/moneytransfer/recent/RecentTransfer.java`
- Create: `src/main/java/com/moneytransfer/recent/RecentTransferRepository.java`

- [ ] **Step 1: Create RecentTransfer entity**

```java
package com.moneytransfer.recent;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "recent_transfers")
public class RecentTransfer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "to_account_id", nullable = false)
    private Long toAccountId;

    @Column(name = "to_account_number", length = 20, nullable = false)
    private String toAccountNumber;

    @Column(name = "to_account_name", length = 255)
    private String toAccountName;

    @Column(name = "transferred_at", nullable = false)
    private LocalDateTime transferredAt;

    public RecentTransfer() {}

    public RecentTransfer(Long userId, Long toAccountId, String toAccountNumber, String toAccountName) {
        this.userId = userId;
        this.toAccountId = toAccountId;
        this.toAccountNumber = toAccountNumber;
        this.toAccountName = toAccountName;
        this.transferredAt = LocalDateTime.now();
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }
    public String getToAccountName() { return toAccountName; }
    public void setToAccountName(String toAccountName) { this.toAccountName = toAccountName; }
    public LocalDateTime getTransferredAt() { return transferredAt; }
    public void setTransferredAt(LocalDateTime transferredAt) { this.transferredAt = transferredAt; }
}
```

- [ ] **Step 2: Create RecentTransferRepository**

```java
package com.moneytransfer.recent;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RecentTransferRepository extends JpaRepository<RecentTransfer, Long> {
    List<RecentTransfer> findTop5ByUserIdOrderByTransferredAtDesc(Long userId);
}
```

- [ ] **Step 3: Create RecentTransferService**

```java
package com.moneytransfer.recent;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RecentTransferService {
    private final RecentTransferRepository repository;

    public RecentTransferService(RecentTransferRepository repository) {
        this.repository = repository;
    }

    public void recordTransfer(Long userId, Long toAccountId, String toAccountNumber, String toAccountName) {
        // Remove old entry for same account to avoid duplicates
        List<RecentTransfer> existing = repository.findTop5ByUserIdOrderByTransferredAtDesc(userId);
        for (RecentTransfer rt : existing) {
            if (rt.getToAccountId().equals(toAccountId)) {
                repository.delete(rt);
            }
        }
        repository.save(new RecentTransfer(userId, toAccountId, toAccountNumber, toAccountName));
    }

    public List<RecentTransfer> getRecentTransfers(Long userId) {
        return repository.findTop5ByUserIdOrderByTransferredAtDesc(userId);
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/moneytransfer/recent/
git commit -m "feat: create RecentTransfer entity, repo, service"
```

---

### Task 8: Record RecentTransfer after successful transfers and create API endpoint

**Files:**
- Modify: `src/main/java/com/moneytransfer/transaction/TransactionController.java`
- Create: `src/main/java/com/moneytransfer/recent/RecentTransferController.java`

- [ ] **Step 1: Inject RecentTransferService into TransactionController**

Add to TransactionController constructor:

```java
private final RecentTransferService recentTransferService;

// in constructor: this.recentTransferService = recentTransferService;
```

After successful transfer (before `return ResponseEntity.ok(...)`):

```java
Account toAccount = accountRepository.findByAccountNumber(toAccountNumber).orElse(null);
if (toAccount != null) {
    User toUser = userRepository.findById(toAccount.getUserId()).orElse(null);
    String toName = toUser != null ? toUser.getFullName() : "";
    recentTransferService.recordTransfer(userId, toAccount.getId(), toAccountNumber, toName);
}
```

- [ ] **Step 2: Create RecentTransferController**

```java
package com.moneytransfer.recent;

import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recent-transfers")
public class RecentTransferController {
    private final RecentTransferService recentTransferService;

    public RecentTransferController(RecentTransferService recentTransferService) {
        this.recentTransferService = recentTransferService;
    }

    @GetMapping
    public ResponseEntity<?> getRecentTransfers(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        List<RecentTransfer> list = recentTransferService.getRecentTransfers(userId);
        return ResponseEntity.ok(list);
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/transaction/TransactionController.java src/main/java/com/moneytransfer/recent/RecentTransferController.java
git commit -m "feat: record recent transfers and create API endpoint"
```

---

### Task 9: Show recent transfers on transfer page

**Files:**
- Modify: `src/main/resources/templates/transfer.html`

- [ ] **Step 1: Add recent transfers section to transfer.html**

After the form (before the "← Về tổng quan" link), add:

```html
<div class="card" style="margin-top:20px;">
    <div class="card-header"><div class="card-title">🕐 Tài khoản gần đây</div></div>
    <div id="recentList" style="display:flex;flex-wrap:wrap;gap:8px;">Đang tải...</div>
</div>
```

Add JS after the accounts fetch:

```javascript
apiFetch('/api/recent-transfers').then(function(res) {
    if (res.ok && res.data.length) {
        var list = document.getElementById('recentList');
        list.innerHTML = '';
        res.data.forEach(function(rt) {
            var btn = document.createElement('button');
            btn.type = 'button';
            btn.className = 'btn btn-outline btn-sm';
            btn.style.margin = '0';
            btn.innerHTML = '🏦 ' + rt.toAccountNumber + (rt.toAccountName ? ' (' + rt.toAccountName + ')' : '');
            btn.onclick = function() { document.getElementById('toAccount').value = rt.toAccountNumber; };
            list.appendChild(btn);
        });
    } else {
        document.getElementById('recentList').textContent = 'Chưa có giao dịch nào';
    }
});
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/transfer.html
git commit -m "feat: show recent transfer accounts on transfer page"
```

---

### Task 10: Add QR code to settings page

**Files:**
- Modify: `src/main/resources/templates/settings.html`

- [ ] **Step 1: Read settings.html**

Read the current settings page to understand structure.

- [ ] **Step 2: Add QR card to settings.html**

After the existing settings cards, add:

```html
<div class="card" style="margin-top:20px;">
    <div class="card-header"><div class="card-title">📱 Mã QR của tôi</div></div>
    <p style="color:var(--text-muted);font-size:0.9rem;margin-bottom:16px;">Quét mã QR này để người khác chuyển tiền cho bạn</p>
    <div id="myQR" style="text-align:center;padding:20px;background:#fff;border-radius:var(--radius-sm);display:inline-block;width:100%;">Đang tải...</div>
    <div id="myAccountNumber" style="text-align:center;margin-top:12px;font-size:0.85rem;color:var(--text-muted);"></div>
</div>
```

JS to generate QR:

```html
<script src="https://cdn.jsdelivr.net/npm/qrcodejs@1.0.0/qrcode.min.js"></script>
<script>
(function() {
    // First get accounts to know the account number
    apiFetch('/api/accounts').then(function(res) {
        if (res.ok && res.data.length) {
            var acct = res.data[0];
            document.getElementById('myAccountNumber').textContent = 'STK: ' + acct.accountNumber;
            var qrDiv = document.getElementById('myQR');
            qrDiv.innerHTML = '';
            new QRCode(qrDiv, {
                text: 'acct:' + acct.accountNumber,
                width: 180,
                height: 180,
                colorDark: '#0f172a',
                colorLight: '#ffffff',
                correctLevel: QRCode.CorrectLevel.H
            });
        } else {
            document.getElementById('myQR').textContent = 'Chưa có tài khoản';
        }
    });
})();
</script>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/settings.html
git commit -m "feat: add QR code to settings page"
```

---

### Task 11: Add QR scan button to transfer page

**Files:**
- Modify: `src/main/resources/templates/transfer.html`

- [ ] **Step 1: Add QR scan button next to "Đến tài khoản" input**

Find the "Đến tài khoản" form-group. After the input, add a scan button:

```html
<button type="button" id="scanQrBtn" class="btn btn-outline btn-sm" style="margin-top:8px;">📷 Quét mã QR</button>
```

- [ ] **Step 2: Add QR scan JS**

Add before the form submit handler:

```html
<script src="https://cdn.jsdelivr.net/npm/jsqr@1.4.0/dist/jsQR.js"></script>
<script>
document.getElementById('scanQrBtn').addEventListener('click', function() {
    openCamera(function(base64) {
        var img = new Image();
        img.onload = function() {
            var canvas = document.createElement('canvas');
            canvas.width = img.width;
            canvas.height = img.height;
            var ctx = canvas.getContext('2d');
            ctx.drawImage(img, 0, 0);
            var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
            var code = jsQR(imageData.data, imageData.width, imageData.height);
            if (code && code.data.startsWith('acct:')) {
                var acctNumber = code.data.substring(5);
                document.getElementById('toAccount').value = acctNumber;
                showToast('Đã quét mã: ' + acctNumber, 'success');
            } else {
                showToast('Không tìm thấy mã QR hợp lệ', 'error');
            }
        };
        img.src = 'data:image/jpeg;base64,' + base64;
    });
});
</script>
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/templates/transfer.html
git commit -m "feat: add QR scan button to transfer page"
```

---

### Task 12: Build and verify

**Files:** (none)

- [ ] **Step 1: Compile project**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Restart app and test**

Start app, then verify:
- Login as user without PIN → goes directly to dashboard
- Login as user with PIN → goes to /pin-verify first
- Settings page shows QR code
- Transfer page shows scan button + recent transfers list
- Transfer with PIN set → PIN modal appears first
- After transfer → new entry in recent transfers
- Scan QR code on transfer page → fills account number
