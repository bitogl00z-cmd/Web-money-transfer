# UI Enhancements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add collapsible sidebar, dark/light mode toggle, remove beneficiary/limit items, and add face+pin dual verification for transfers >= 10M VND.

**Architecture:** Pure frontend changes to existing HTML templates, CSS, and JS files. No backend modifications needed — the backend already has `POST /api/face/verify-for-transfer` and `TransactionController` already validates `faceToken` for amounts > 10M.

**Tech Stack:** Thymeleaf templates, vanilla CSS, vanilla JS.

---

### Task 1: Add light theme CSS variables and collapsed sidebar styles

**Files:**
- Modify: `src/main/resources/static/css/banking.css`

- [ ] **Step 1: Add `.light-mode` CSS class with light theme variables after `:root` block**

After the closing `}` of `:root` at line 23, insert:

```css
.light-mode {
    --bg: #f8fafc;
    --bg-elevated: #ffffff;
    --card: #ffffff;
    --text: #1e293b;
    --text-muted: #64748b;
    --text-dim: #94a3b8;
    --primary: #2563eb;
    --primary-dark: #1d4ed8;
    --primary-glow: rgba(37,99,235,0.12);
    --border: #e2e8f0;
    --border-light: #cbd5e1;
    --shadow: 0 4px 24px rgba(0,0,0,0.08);
    --shadow-lg: 0 8px 40px rgba(0,0,0,0.12);
}
```

- [ ] **Step 2: Add collapsed sidebar CSS at the end of the file (before the last closing)**

Before the last `@keyframes` blocks, add:

```css
/* Collapsed sidebar */
.sidebar.collapsed { width: 60px; }
.sidebar.collapsed .sidebar-item { justify-content: center; padding: 12px; margin: 2px 4px; }
.sidebar.collapsed .sidebar-item span:not(.icon) { display: none; }
.sidebar.collapsed .sidebar-item .icon { margin: 0; font-size: 1.3rem; }
.sidebar.collapsed hr { margin: 8px 4px; }
.main-content.expanded { margin-left: 60px; }

/* Sidebar collapse toggle button */
.sidebar-toggle {
    background: none; border: none; color: var(--text-muted); cursor: pointer;
    font-size: 1.1rem; padding: 6px 10px; border-radius: var(--radius-sm);
    transition: var(--transition); margin-right: 4px;
}
.sidebar-toggle:hover { background: var(--primary-glow); color: var(--text); }

/* Theme toggle button */
.theme-toggle {
    background: none; border: 1.5px solid var(--border-light); cursor: pointer;
    font-size: 1rem; padding: 6px 10px; border-radius: var(--radius-sm);
    transition: var(--transition); color: var(--text-muted); line-height: 1;
}
.theme-toggle:hover { background: var(--primary-glow); color: var(--text); border-color: var(--primary); }
```

- [ ] **Step 3: Update navbar `.hamburger` to `.hamburger-btn` for desktop display**

Replace `.navbar .hamburger { display: none !important; }` at line 245 with:
```css
@media (min-width: 769px) { .hamburger-btn, .sidebar-toggle { display: inline-flex !important; } }
```

Also update the existing mobile rule at line 239 to not hide the hamburger:
```css
@media (max-width: 768px) {
    .sidebar { transform: translateX(-100%); }
    .sidebar.open { transform: translateX(0); }
    .main-content { margin-left: 0; padding: 20px; }
    .hamburger-btn { display: inline-flex !important; }
    .sidebar-toggle { display: none !important; }
    .stat-grid { grid-template-columns: 1fr; }
}
```

- [ ] **Step 4: Run `mvn compile` to verify CSS path is valid (no Java compilation needed)**

---

### Task 2: Add JS functions for sidebar collapse and theme toggle

**Files:**
- Modify: `src/main/resources/static/js/banking.js`

- [ ] **Step 1: Add `toggleCollapse()` function after `toggleSidebar()` at line 125**

```javascript
function toggleCollapse() {
    var sidebar = document.querySelector('.sidebar');
    var main = document.querySelector('.main-content');
    sidebar.classList.toggle('collapsed');
    main.classList.toggle('expanded');
    localStorage.setItem('sidebarCollapsed', sidebar.classList.contains('collapsed') ? 'true' : 'false');
}
```

- [ ] **Step 2: Add `toggleTheme()` function**

```javascript
function toggleTheme() {
    var html = document.documentElement;
    var isLight = html.classList.toggle('light-mode');
    localStorage.setItem('theme', isLight ? 'light' : 'dark');
    var btn = document.getElementById('themeToggleBtn');
    if (btn) btn.textContent = isLight ? '🌙' : '☀️';
}
```

- [ ] **Step 3: Add DOMContentLoaded logic to restore saved states**

Append at end of existing DOMContentLoaded listener (before closing `});` at line 171), add:
```javascript
// Restore sidebar collapsed state
if (localStorage.getItem('sidebarCollapsed') === 'true') {
    document.querySelector('.sidebar')?.classList.add('collapsed');
    document.querySelector('.main-content')?.classList.add('expanded');
}
// Restore theme
if (localStorage.getItem('theme') === 'light') {
    document.documentElement.classList.add('light-mode');
    var btn = document.getElementById('themeToggleBtn');
    if (btn) btn.textContent = '🌙';
}
```

---

### Task 3: Update layout.html — add toggle buttons, remove beneficiary

**Files:**
- Modify: `src/main/resources/templates/fragments/layout.html`

- [ ] **Step 1: Add collapse and theme toggle buttons in the navbar**

After the hamburger button (line 13), add:
```html
<button class="sidebar-toggle" onclick="toggleCollapse()" title="Thu gọn sidebar">◀</button>
```

After `</a>` closing the logout link (line 20), before `</div>`, add:
```html
<button id="themeToggleBtn" class="theme-toggle" onclick="toggleTheme()" title="Đổi màu nền">☀️</button>
```

- [ ] **Step 2: Remove the beneficiary menu item (lines 39-41)**

Delete:
```html
        <a th:href="@{/beneficiaries}" th:classappend="${currentPage == 'beneficiaries'} ? 'active'" class="sidebar-item">
            <span class="icon">👥</span> Thụ hưởng
        </a>
```

- [ ] **Step 3: Update hamburger button class for styling**

Replace `<button class="hamburger btn"` with `<button class="hamburger-btn btn"` at line 13.

---

### Task 4: Remove limits tab from settings.html

**Files:**
- Modify: `src/main/resources/templates/settings.html`

- [ ] **Step 1: Remove the limits tab button from the tab bar (line 17)**

Delete:
```html
            <button class="tab-btn" data-tab="limits">💰 Hạn mức</button>
```

- [ ] **Step 2: Remove the limits tab content (lines 64-72)**

Delete the entire `<div id="tab-limits" class="tab-content">...</div>` block:
```html
        <div id="tab-limits" class="tab-content">
            <div class="card">
                <div class="card-header"><div class="card-title">💰 Hạn mức giao dịch</div></div>
                <div class="card-body">
                    <div style="margin-bottom:12px;"><strong style="color:var(--text-muted);font-size:0.85rem;">Hạng thành viên</strong><br><span id="l-tier" class="badge badge-info">STANDARD</span></div>
                    <div><strong style="color:var(--text-muted);font-size:0.85rem;">Bảo mật giao dịch</strong><br>Mọi giao dịch chuyển tiền đều yêu cầu xác thực mã PIN</div>
                </div>
            </div>
        </div>
```

---

### Task 5: Add face verification step in transfer.html for >= 10M

**Files:**
- Modify: `src/main/resources/templates/transfer.html`

- [ ] **Step 1: Replace the submit handler's PIN-only flow with dual verification for >= 10M**

In `transfer.html`, find the submit handler at lines 139-190. Replace the section from the `if (userPinSet)` check (line 161) with the following:

Replace lines 161-165:
```javascript
            if (userPinSet) {
                currentPinToken = await requirePin();
                if (!currentPinToken) { btn.disabled = false; btn.textContent = '💸 Chuyển tiền'; return; }
                headers['x-pin-token'] = currentPinToken;
            }
```

With:
```javascript
            if (userPinSet) {
                currentPinToken = await requirePin();
                if (!currentPinToken) { btn.disabled = false; btn.textContent = '💸 Chuyển tiền'; return; }
                headers['x-pin-token'] = currentPinToken;
            }

            // Face verification for amounts >= 10,000,000
            var faceToken = null;
            if (amount >= 10000000) {
                var faceResult = await new Promise(function(resolve) {
                    openCamera(function(base64) { resolve(base64); });
                });
                if (!faceResult) { btn.disabled = false; btn.textContent = '💸 Chuyển tiền'; return; }
                var faceRes = await apiFetch('/api/face/verify-for-transfer', { method: 'POST', body: { faceImage: faceResult } });
                if (faceRes.ok && faceRes.data.faceToken) {
                    faceToken = faceRes.data.faceToken;
                    body.faceToken = faceToken;
                } else {
                    showToast('Xác thực gương mặt thất bại', 'error');
                    btn.disabled = false; btn.textContent = '💸 Chuyển tiền'; return;
                }
            }
```

- [ ] **Step 2: Verify the changes work together**

The full submit flow after changes should be:
1. Validate form fields
2. If user has PIN set: prompt for PIN, set `x-pin-token` header
3. If amount >= 10M: open camera, face verify, set `body.faceToken`
4. Call `POST /api/transactions/transfer` with PIN header + face token in body
5. On success: redirect to receipt
6. On failure: show error message

- [ ] **Step 3: Clean up unused `userFaceEnabled` reference in the profile fetch at line 106**

Change line 106:
```javascript
if (res.ok) { userFaceEnabled = res.data.faceEnabled; userPinSet = res.data.pinSet; }
```
To:
```javascript
if (res.ok) { userPinSet = res.data.pinSet; }
```

---

### Task 6: Restart app and verify

**Files:** None

- [ ] **Step 1: Kill any existing Java process on port 8080**

Run:
```powershell
$p = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue; if ($p) { Stop-Process -Id $p.OwningProcess -Force }
```

- [ ] **Step 2: Restart the application**

Run:
```powershell
Start-Process -NoNewWindow -FilePath "cmd" -ArgumentList "/c mvn spring-boot:run"
```

- [ ] **Step 3: Verify in browser at http://localhost:8080/dashboard**
  - Sidebar collapse toggle works (navbar button)
  - Theme toggle works (dark ↔ light)
  - No "Thụ hưởng" in sidebar
  - No "Hạn mức" tab in Settings
  - Try transfer >= 10M: PIN prompt → camera → success
  - Try transfer < 10M: only PIN prompt as before
