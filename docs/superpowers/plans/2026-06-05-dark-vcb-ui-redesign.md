# Dark VCB UI Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Redesign the banking web app UI from light theme to dark VCB-inspired theme via CSS overhaul with minor template adjustments.

**Architecture:** Single CSS file (`banking.css`) contains all dark theme variables and component styles. Template changes are limited to inline styles in login/register and navbar gradient/sidebar active state. No backend, no JS, no HTML structure changes.

**Tech Stack:** CSS custom properties (variables), Tailwind CSS CDN (unchanged), existing HTML templates with Thymeleaf.

**Spec:** `docs/superpowers/specs/2026-06-05-dark-vcb-ui-redesign.md`

---

### Task 1: Update CSS root variables for dark theme

**Files:**
- Modify: `src/main/resources/static/css/banking.css:1-21`

- [ ] **Step 1: Replace CSS variables for dark palette**

Replace the `:root` block with dark theme color tokens:

```css
:root {
    --bg: #0f172a;
    --bg-elevated: #1a2332;
    --card: #1e293b;
    --card-hover: #334155;
    --text: #f1f5f9;
    --text-muted: #94a3b8;
    --text-dim: #64748b;
    --primary: #3b82f6;
    --primary-dark: #2563eb;
    --primary-glow: rgba(59,130,246,0.15);
    --success: #10b981;
    --danger: #ef4444;
    --warning: #f59e0b;
    --border: #1e293b;
    --border-light: #334155;
    --radius: 12px;
    --radius-sm: 8px;
    --shadow: 0 4px 24px rgba(0,0,0,0.25);
    --shadow-lg: 0 8px 40px rgba(0,0,0,0.35);
    --transition: 0.2s ease;
    --sidebar-width: 240px;
    --nav-height: 64px;
}
```

- [ ] **Step 2: Verify no missing variable references**

Search the CSS file for any other uses of `--primary-dark` or `--primary-bg` that need updating.

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark theme CSS variables"
```

---

### Task 2: Navbar dark styling

**Files:**
- Modify: `src/main/resources/static/css/banking.css:33-50`

- [ ] **Step 1: Update navbar gradient to dark**

Replace existing navbar styles:

```css
.navbar {
    position: fixed; top: 0; left: 0; right: 0;
    height: var(--nav-height);
    background: linear-gradient(135deg, #0c1427, #1e293b);
    color: #f1f5f9;
    display: flex; align-items: center;
    padding: 0 24px;
    z-index: 100;
    border-bottom: 1px solid var(--border-light);
    box-shadow: 0 1px 12px rgba(59,130,246,0.08);
}
.navbar .logo { font-size: 1.25rem; font-weight: 700; display: flex; align-items: center; gap: 10px; }
.navbar .logo-icon { font-size: 1.5rem; }
.navbar .logo span:last-child { color: var(--primary); }
.navbar .nav-right { margin-left: auto; display: flex; align-items: center; gap: 16px; }
.navbar .nav-link { color: #94a3b8; text-decoration: none; font-size: 0.9rem; transition: var(--transition); }
.navbar .nav-link:hover { color: #f1f5f9; }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark navbar"
```

---

### Task 3: Sidebar dark styling

**Files:**
- Modify: `src/main/resources/static/css/banking.css:53-74`

- [ ] **Step 1: Rewrite sidebar styles**

```css
.sidebar {
    position: fixed; top: var(--nav-height); left: 0; bottom: 0;
    width: var(--sidebar-width);
    background: #0f172a;
    border-right: 1px solid var(--border);
    padding: 12px 0;
    overflow-y: auto;
    z-index: 50;
    transition: transform var(--transition);
}
.sidebar-item {
    display: flex; align-items: center; gap: 12px;
    padding: 12px 24px;
    margin: 2px 8px;
    border-radius: var(--radius-sm);
    color: var(--text-muted);
    text-decoration: none;
    font-size: 0.9rem;
    transition: var(--transition);
}
.sidebar-item:hover { background: rgba(59,130,246,0.08); color: #f1f5f9; }
.sidebar-item.active { background: var(--primary-glow); color: var(--primary); font-weight: 600; }
.sidebar-item .icon { font-size: 1.1rem; width: 24px; text-align: center; }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark sidebar"
```

---

### Task 4: Card and stat card dark styling

**Files:**
- Modify: `src/main/resources/static/css/banking.css:85-117`

- [ ] **Step 1: Rewrite card styles**

```css
.card {
    background: var(--card);
    border-radius: var(--radius);
    border: 1px solid var(--border);
    padding: 24px;
    transition: var(--transition);
}
.card:hover { border-color: var(--border-light); box-shadow: var(--shadow-lg); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.card-title { font-size: 1.1rem; font-weight: 700; color: var(--text); }
.card-subtitle { font-size: 0.85rem; color: var(--text-muted); }
```

- [ ] **Step 2: Rewrite stat card styles**

```css
.stat-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 32px; }
.stat-card {
    background: var(--card);
    border-radius: var(--radius);
    border: 1px solid var(--border);
    padding: 20px 24px;
    transition: var(--transition);
    position: relative; overflow: hidden;
}
.stat-card::before {
    content: ''; position: absolute; top: 0; left: 0; bottom: 0; width: 4px;
}
.stat-card.blue::before { background: var(--primary); }
.stat-card.green::before { background: var(--success); }
.stat-card.orange::before { background: var(--warning); }
.stat-card.purple::before { background: #8b5cf6; }
.stat-card.teal::before { background: #14b8a6; }
.stat-card.red::before { background: var(--danger); }
.stat-card:hover { border-color: var(--border-light); box-shadow: var(--shadow-lg); transform: translateY(-2px); }
.stat-label { font-size: 0.85rem; color: var(--text-muted); margin-bottom: 4px; }
.stat-value { font-size: 1.75rem; font-weight: 700; color: var(--text); }
.stat-sub { font-size: 0.8rem; color: var(--text-dim); margin-top: 4px; }
```

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark cards and stat cards"
```

---

### Task 5: Button dark styling

**Files:**
- Modify: `src/main/resources/static/css/banking.css:120-140`

- [ ] **Step 1: Rewrite button styles**

```css
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
.btn-primary:hover { background: var(--primary-dark); transform: translateY(-1px); box-shadow: 0 4px 12px rgba(59,130,246,0.3); }
.btn-success { background: var(--success); color: #fff; }
.btn-success:hover { background: #059669; transform: translateY(-1px); }
.btn-danger { background: var(--danger); color: #fff; }
.btn-danger:hover { background: #dc2626; }
.btn-warning { background: var(--warning); color: #0f172a; }
.btn-warning:hover { background: #d97706; }
.btn-outline { background: transparent; color: var(--primary); border: 1.5px solid var(--primary); }
.btn-outline:hover { background: var(--primary-glow); }
.btn-sm { padding: 6px 16px; font-size: 0.85rem; }
.btn-lg { padding: 14px 32px; font-size: 1rem; }
.btn-full { width: 100%; }
.btn:disabled { opacity: 0.5; cursor: not-allowed; transform: none; }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark buttons"
```

---

### Task 6: Form input dark styling

**Files:**
- Modify: `src/main/resources/static/css/banking.css:143-156`

- [ ] **Step 1: Rewrite form styles**

```css
.form-group { margin-bottom: 16px; }
.form-label { display: block; font-size: 0.85rem; font-weight: 600; color: var(--text-muted); margin-bottom: 6px; }
.form-input {
    width: 100%; padding: 10px 14px;
    border: 1.5px solid var(--border-light);
    border-radius: var(--radius-sm);
    font-size: 0.9rem;
    transition: var(--transition);
    outline: none;
    background: #0f172a;
    color: var(--text);
}
.form-input::placeholder { color: var(--text-dim); }
.form-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow); }
.form-input.error { border-color: var(--danger); }
.form-select { appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%2394a3b8' d='M6 8L1 3h10z'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 12px center; padding-right: 36px; }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark form inputs"
```

---

### Task 7: Table dark styling

**Files:**
- Modify: `src/main/resources/static/css/banking.css:158-173`

- [ ] **Step 1: Rewrite table styles**

```css
.table-container { overflow-x: auto; }
.table { width: 100%; border-collapse: collapse; font-size: 0.9rem; }
.table th {
    text-align: left; padding: 12px 16px;
    font-weight: 600; color: var(--text-muted); font-size: 0.8rem;
    text-transform: uppercase; letter-spacing: 0.5px;
    border-bottom: 2px solid var(--border-light);
    background: #0f172a;
}
.table td { padding: 12px 16px; border-bottom: 1px solid var(--border); color: var(--text); }
.table tr:hover td { background: rgba(59,130,246,0.06); }
.table .empty { text-align: center; padding: 40px; color: var(--text-dim); }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark tables"
```

---

### Task 8: Badges, modal, toast dark styling

**Files:**
- Modify: `src/main/resources/static/css/banking.css:175-225`

- [ ] **Step 1: Rewrite badge styles**

```css
.badge {
    display: inline-block; padding: 3px 10px;
    border-radius: 20px; font-size: 0.75rem; font-weight: 600;
}
.badge-success { background: rgba(16,185,129,0.15); color: #34d399; }
.badge-danger { background: rgba(239,68,68,0.15); color: #f87171; }
.badge-warning { background: rgba(245,158,11,0.15); color: #fbbf24; }
.badge-info { background: rgba(59,130,246,0.15); color: #60a5fa; }
.badge-purple { background: rgba(139,92,246,0.15); color: #a78bfa; }
.badge-gray { background: rgba(148,163,184,0.15); color: #cbd5e1; }
```

- [ ] **Step 2: Rewrite modal styles**

```css
.modal-overlay {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.7);
    display: flex; align-items: center; justify-content: center;
    z-index: 1000;
    animation: fadeIn 0.2s;
}
.modal {
    background: var(--bg-elevated);
    border: 1px solid var(--border-light);
    border-radius: var(--radius);
    box-shadow: var(--shadow-lg);
    padding: 32px;
    max-width: 480px; width: 90%;
    max-height: 90vh; overflow-y: auto;
    animation: slideUp 0.3s;
}
.modal-title { font-size: 1.2rem; font-weight: 700; margin-bottom: 8px; color: var(--text); }
.modal-desc { font-size: 0.9rem; color: var(--text-muted); margin-bottom: 20px; }
.modal-actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 20px; }
```

- [ ] **Step 3: Rewrite toast styles**

```css
.toast-container { position: fixed; top: 80px; right: 20px; z-index: 2000; display: flex; flex-direction: column; gap: 8px; }
.toast {
    padding: 12px 20px; border-radius: var(--radius-sm);
    color: #fff; font-size: 0.9rem; font-weight: 500;
    box-shadow: var(--shadow-lg);
    animation: slideIn 0.3s;
    display: flex; align-items: center; gap: 8px;
}
.toast-success { background: #065f46; }
.toast-error { background: #991b1b; }
.toast-info { background: #1e40af; }
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: dark badges, modal, toasts"
```

---

### Task 9: Update page headings, camera, chart, spinner dark styles

**Files:**
- Modify: `src/main/resources/static/css/banking.css:227-259`

- [ ] **Step 1: Update remaining component styles**

```css
.page-header { margin-bottom: 24px; }
.page-title { font-size: 1.5rem; font-weight: 700; color: var(--text); }
.page-subtitle { font-size: 0.9rem; color: var(--text-muted); margin-top: 4px; }

.grid-2 { display: grid; grid-template-columns: repeat(2, 1fr); gap: 20px; }
.grid-3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
@media (max-width: 768px) { .grid-2, .grid-3 { grid-template-columns: 1fr; } }

@media (max-width: 768px) {
    .sidebar { transform: translateX(-100%); }
    .sidebar.open { transform: translateX(0); }
    .main-content { margin-left: 0; padding: 20px; }
    .navbar .hamburger { display: block !important; }
    .stat-grid { grid-template-columns: 1fr; }
}
@media (min-width: 769px) { .navbar .hamburger { display: none !important; } }

@keyframes fadeIn { from { opacity: 0; } to { opacity: 1; } }
@keyframes slideUp { from { transform: translateY(20px); opacity: 0; } to { transform: translateY(0); opacity: 1; } }
@keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }

.chart-container { position: relative; height: 300px; }
.chart-container canvas { filter: brightness(0.9); }

.spinner { width: 24px; height: 24px; border: 3px solid var(--border-light); border-top-color: var(--primary); border-radius: 50%; animation: spin 0.6s linear infinite; display: inline-block; }
@keyframes spin { to { transform: rotate(360deg); } }

.camera-container { position: relative; width: 100%; max-width: 360px; margin: 0 auto; border-radius: var(--radius-sm); overflow: hidden; background: #000; border: 1px solid var(--border-light); }
.camera-container video { width: 100%; display: block; }
.camera-overlay { position: absolute; top: 0; left: 0; right: 0; bottom: 0; border: 3px solid var(--primary); border-radius: var(--radius-sm); pointer-events: none; }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/static/css/banking.css
git commit -m "style: remaining dark component styles"
```

---

### Task 10: Update login page for dark theme

**Files:**
- Modify: `src/main/resources/templates/login.html:4-5`

- [ ] **Step 1: Change inline background style**

Find the `body` tag with inline style and change the background gradient to dark:

```html
<body style="background:linear-gradient(135deg, #0f172a, #1e293b);min-height:100vh;display:flex;align-items:center;justify-content:center;padding:20px;">
```

- [ ] **Step 2: Update card container style**

Add dark card border and shadow:

```html
<div style="background:var(--card);border-radius:var(--radius);border:1px solid var(--border-light);box-shadow:var(--shadow-lg);padding:40px;max-width:420px;width:100%;">
```

- [ ] **Step 3: Update text colors**

Change the "Ngân Hàng" heading and subtitle text for dark background:
```html
<h1 style="font-size:1.5rem;font-weight:700;color:#f1f5f9;">Ngân Hàng</h1>
<p style="color:#94a3b8;font-size:0.9rem;margin-top:4px;">Đăng nhập để tiếp tục</p>
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/templates/login.html
git commit -m "style: dark login theme"
```

---

### Task 11: Update register page for dark theme

**Files:**
- Modify: `src/main/resources/templates/register.html` (if it exists)

- [ ] **Step 1: Apply same dark theme changes as login page**

Apply the same background, card, and text color changes that were made to `login.html`.

- [ ] **Step 2: Commit**

```bash
git add src/main/resources/templates/register.html
git commit -m "style: dark register theme"
```

---

### Task 12: Build and verify

**Files:** (none — verification step)

- [ ] **Step 1: Compile the project**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 2: Restart application and verify pages render correctly**

Start the app, then manually verify:
- Login page loads with dark theme
- Register page loads with dark theme
- Dashboard shows stat cards with dark styling
- Transfer page form looks correct
- Admin page tables render with dark styling
- Sidebar navigation active/hover states work
- Buttons, badges, modals all display correctly
