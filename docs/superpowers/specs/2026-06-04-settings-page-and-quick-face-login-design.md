# Settings Page & Quick Face Login — Design Spec

## Overview

Replace the existing Profile page (`/profile`) with a full Settings page (`/settings`) resembling a real banking app, and add quick face login (no username required) using LBPH recognition.

## Data Model Changes

### User.java — new fields

| Field | Type | Default | Purpose |
|---|---|---|---|
| `emailNotifications` | `boolean` | `true` | Notifications section toggle |
| `language` | `String` (length 5) | `"vi"` | Language preference |

No changes to existing face fields (`faceEnabled`, `faceEncoding`, `faceImageUrl`).

## API Endpoints

### New endpoints

| Method | Path | Request Body | Response | Purpose |
|---|---|---|---|---|
| `POST` | `/api/auth/change-password` | `{ oldPassword, newPassword }` | `{ message }` or 400 error | Change password (verify old, hash new) |
| `POST` | `/api/auth/face-quick-login` | `{ faceImage }` | `{ message }` + JWT cookies or 400 error | Quick login — identifies user by face only |

### Modified endpoints

| Method | Path | Change |
|---|---|---|
| `PUT` | `/api/users/profile` | Add `emailNotifications`, `language` to allowed update fields |

## Engine Changes (JavaCVFaceEngine)

Add method:
```java
public Optional<Long> identifyFace(byte[] imageBytes)
```
- Decodes image, detects face, preprocesses, calls `FaceRecognizer.predict()`
- If confidence < threshold (80.0), returns `Optional.of(userId)` from label
- Otherwise returns `Optional.empty()`

This enables quick login without sending userId/username.

## AuthController — Change Password

New method `changePassword(Authentication auth, @RequestBody Map<String,String> body)`:
1. Extract userId from JWT
2. Validate `oldPassword` matches `passwordHash`
3. Validate `newPassword` length ≥ 6
4. Hash new password, update `User.passwordHash`
5. Return success message

## AuthController — Quick Face Login

New method `quickFaceLogin(@RequestBody Map<String,String> body)`:
1. Extract `faceImage` from body
2. Call `faceService.identifyFace(faceImage)` (new method delegating to engine)
3. If not found → return 400 "Không nhận diện được khuôn mặt"
4. Look up user by ID
5. Check `faceEnabled`, account lock
6. Generate JWT tokens, set HttpOnly cookies
7. Return success → redirect to dashboard

## FaceService — new method

```java
public User identifyFace(String base64Image)
```
- Decodes base64 → bytes
- Calls `javaCVFaceEngine.identifyFace(bytes)`
- Returns the matched User entity or throws

## Frontend

### Template changes

- **DELETE** `profile.html`
- **ADD** `settings.html` (replaces profile)
- **MODIFY** `login.html` — add "Đăng nhập nhanh" button
- **MODIFY** `fragments/layout.html` — rename "Hồ sơ" → "Cài đặt", update href from `/profile` → `/settings`
- **MODIFY** `banking.js` — add any shared helpers

### Settings page layout

Single page with 6 tabbed sections, JS tab switching:

1. **Thông tin cá nhân** — input fields for fullName, email, phone; Save button → `PUT /api/users/profile`
2. **Bảo mật** — password change form (3 fields: old, new, confirm) + OTP toggle checkbox
3. **Đăng nhập nhanh** — face status badge, register/update button, enable/disable toggle
4. **Hạn mức giao dịch** — read-only: tier display, face verify threshold info
5. **Thông báo** — checkbox for email notifications → `PUT /api/users/profile`
6. **Ngôn ngữ** — dropdown (Tiếng Việt / English) → saves to `language` field

### Face registration moved to Settings tab #3

Same camera flow (`openCamera()` → base64 → `POST /api/face/register`) but now lives in settings instead of profile.

### Login page changes

Add button below existing face login button:
```html
<button onclick="quickFaceLogin()">📸 Đăng nhập nhanh</button>
```

JS function `quickFaceLogin()`:
1. Open camera
2. Send `{ faceImage: base64 }` to `POST /api/auth/face-quick-login`
3. On success → `window.location.href = '/dashboard'`
4. On error → show toast

## Security

- `POST /api/auth/face-quick-login` — **public** (no auth), rate limited implicitly by face processing time
- `POST /api/auth/change-password` — authenticated (JWT)
- Face quick login checks account lockout (`failedAttempts ≥ 5`)
- Max 5 failed face attempts → account locked

## Files to Create/Modify

### Create
- `src/main/resources/templates/settings.html`

### Modify
- `src/main/java/com/moneytransfer/user/User.java` — add emailNotifications, language
- `src/main/java/com/moneytransfer/face/JavaCVFaceEngine.java` — add identifyFace()
- `src/main/java/com/moneytransfer/face/FaceService.java` — add identifyFace() + identifyFaceBase64Sync()
- `src/main/java/com/moneytransfer/auth/AuthController.java` — add changePassword(), quickFaceLogin()
- `src/main/java/com/moneytransfer/user/UserController.java` — update allowed fields
- `src/main/resources/templates/login.html` — add quick login button
- `src/main/resources/templates/fragments/layout.html` — rename + relink

### Delete
- `src/main/resources/templates/profile.html`
- `src/main/java/com/moneytransfer/user/UserController.java` — remove GET/PUT /api/users/profile if only used by profile page (keep if used elsewhere)

## Testing

- Existing 23 tests should still pass
- FaceService test: add test for identifyFace
- AuthController test: add tests for changePassword + quickFaceLogin
- Manual test: register face → login with quick face → verify dashboard loads
