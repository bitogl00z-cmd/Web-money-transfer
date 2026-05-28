# Banking UI Redesign — Design Spec

## Overview
Redesign the Money Transfer web app with a professional banking UI (blue & white), VND currency, face login, and face verification for transfers > 10,000,000₫.

## 1. Visual Style
- **Theme:** Modern blue & white (TPBank/VPBank style)
- **Primary:** `#1e3a5f` (nav), `#2563eb` (buttons), `#3b82f6` (accents)
- **Success:** `#10b981`, **Danger:** `#ef4444`, **Warning:** `#f59e0b`
- **Background:** `#f0f4f8` (light gray-blue)
- **Cards:** `#ffffff`, rounded-2xl (16px), shadow-lg, p-6
- **Font:** Inter (Google Fonts)
- **Responsive:** sidebar collapses to hamburger on mobile

## 2. Layout Structure
All pages share `fragments/layout.html`:
```
[Navbar: Logo + Notifications + Profile + Logout]
[Sidebar: Dashboard, Transfer, History, Beneficiaries, Scheduled, Profile, Admin]
[Main content area]
```
- Sidebar: 240px on desktop, hidden + hamburger on mobile
- Navbar: fixed top, gradient #1e3a5f → #2563eb, white text

## 3. Frontend Assets (new files)

### `static/css/banking.css`
- CSS variables for design system
- Navbar, sidebar styles
- Card, button, form, table styles
- Modal overlay + camera popup
- Animations (fadeIn, slideIn)
- Responsive breakpoints

### `static/js/banking.js`
- `formatVND(amount)` — formats number as `1.000.000₫`
- `captureFace(onCapture)` — opens camera modal, captures image, returns base64 string
- `showModal/closeModal` — generic modal helpers
- `showToast(message, type)` — success/error notifications
- `formatDate` — date formatting
- `getCsrfToken` — CSRF helper if needed

## 4. Face Login

### Backend: `POST /api/auth/face-login`
- Request body: `{ username: string, faceImage: string (base64) }`
- Flow: find user by username → verify face (decode base64 → OpenCV) → check match
- Response on success: `{ token: "jwt...", username: "...", fullName: "..." }`
- Response on failure: `{ error: "Face verification failed" }`

### Frontend: login.html
- Existing password form preserved
- New button "Đăng nhập bằng gương mặt" below form
- Click → opens camera modal → captures image → calls `/api/auth/face-login` → on success redirect `/dashboard`

## 5. Transfer Face Check (amount > 10M VND)

### Backend

**`POST /api/face/verify-for-transfer`**
- Request body: `{ faceImage: string (base64) }`
- Response: `{ faceToken: "jwt..." }` (1-minute expiry, claims: `{userId, purpose: "TRANSFER"}`)

**Modified `POST /api/transactions/transfer`**
- If amount > 10,000,000: require `faceToken` in request body
- Validate faceToken (signature, purpose="TRANSFER", userId matches, not expired)
- If missing/invalid: return 400 `{ error: "Face verification required for amounts over 10,000,000₫" }`

### Frontend: transfer.html
- User fills form, clicks "Chuyển tiền"
- If amount > 10M: popup camera → verify face → get faceToken → include in transfer request
- If amount ≤ 10M: normal transfer, no face check

## 6. Currency: VND
- All templates display amounts in VND (₫), formatted via `formatVND()`
- Backend stores values as `BigDecimal` (unchanged)
- Frontend formatting only — no database changes

## 7. VND Format
- `1.000.000₫` (dot separator, ₫ suffix, Vietnamese convention)
- No decimals (VND has no subunits)

## 8. Files Changed

### New files
| File | Purpose |
|------|---------|
| `src/main/resources/static/css/banking.css` | Design system CSS |
| `src/main/resources/static/js/banking.js` | Shared JS utilities |
| `src/main/resources/templates/fragments/layout.html` | Thymeleaf layout fragment |

### Modified files
| File | Changes |
|------|---------|
| `templates/login.html` | Face login button + camera modal |
| `templates/register.html` | Use layout, VND |
| `templates/dashboard.html` | Use layout, VND formatting, Chart.js |
| `templates/transfer.html` | Use layout, face verify modal for >10M |
| `templates/history.html` | Use layout, VND |
| `templates/beneficiaries.html` | Use layout, VND |
| `templates/scheduled.html` | Use layout, VND |
| `templates/profile.html` | Use layout, face registration UI |
| `templates/admin/users.html` | Use layout, VND |
| `templates/admin/transactions.html` | Use layout, VND |
| `AuthController.java` | Add face-login endpoint |
| `FaceController.java` | Add verify-for-transfer endpoint |
| `TransactionController.java` | Check amount > 10M for face token |

## 9. Security
- Face images sent as base64 over HTTPS
- Face token: signed JWT, 1-minute TTL, bound to userId + purpose
- Face encoding stored in DB (already exists)
- All existing auth endpoints preserved

## 10. Dependencies
- No new Maven/Gradle dependencies
- OpenCV already bundled via `org.openpnp:opencv:4.9.0-0`
- Browser `getUserMedia` API for camera access (no extra libs)

## 11. Non-Goals
- No face registration from admin panel
- No face login for admin users (admin must use password)
- No face check for deposit/withdraw (transfer only)
- No biometric data stored outside DB (faceEncoding stays in User entity)
