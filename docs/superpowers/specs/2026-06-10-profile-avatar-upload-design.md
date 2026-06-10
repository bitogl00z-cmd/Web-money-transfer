# Profile Avatar Upload Design

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a real profile avatar upload flow so the user can pick an image from the local machine, store it on the server, and show it consistently in settings, navbar, and dashboard. Face images remain separate and keep their current login/verification role.

**Architecture:** Introduce a dedicated avatar field (`avatarUrl`) on `User`, a multipart upload endpoint for avatars, and a static file serving path for uploaded images. The existing face recognition pipeline stays unchanged and continues to use `faceImageUrl` / face data only for login and high-value transfer verification. UI updates read `avatarUrl` and render a server-hosted image with a fallback state when no avatar exists.

**Tech Stack:** Spring Boot, Spring MVC multipart upload, JPA, Thymeleaf, vanilla JS/CSS.

---

### Task 1: Add avatar storage to the user model

**Files:**
- Modify: `src/main/java/com/moneytransfer/user/User.java`
- Modify: `src/main/java/com/moneytransfer/user/UserService.java`
- Modify: `src/main/java/com/moneytransfer/user/UserController.java`

- [ ] **Step 1: Add `avatarUrl` to `User`**

Add a nullable field separate from face data:

```java
@Column(length = 500)
private String avatarUrl;

public String getAvatarUrl() { return avatarUrl; }
public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
```

- [ ] **Step 2: Return `avatarUrl` in the profile response**

Update the profile JSON map to include `avatarUrl`, while keeping `faceImageUrl` for face features.

- [ ] **Step 3: Persist `avatarUrl` in profile updates only if needed**

Do not overload `faceImageUrl`. Avatar updates should use the dedicated upload endpoint in Task 2.

---

### Task 2: Add server-side avatar upload endpoint

**Files:**
- Modify: `src/main/java/com/moneytransfer/user/UserController.java`
- Create: `src/main/java/com/moneytransfer/config/AvatarStorageConfig.java`

- [ ] **Step 1: Add `POST /api/users/avatar` multipart endpoint**

Accept `MultipartFile avatar` and the authenticated user. Validate:

```java
if (avatar == null || avatar.isEmpty()) return badRequest;
if (!avatar.getContentType().startsWith("image/")) return badRequest;
if (avatar.getSize() > 5 * 1024 * 1024) return badRequest;
```

- [ ] **Step 2: Save the file to a server folder**

Use a deterministic server-owned directory like `uploads/avatars/` under the project root. Generate a unique filename such as `userId-uuid.ext`.

- [ ] **Step 3: Store the public URL in `avatarUrl`**

After saving, persist the relative URL (for example `/uploads/avatars/userId-uuid.jpg`) to the user row.

- [ ] **Step 4: Serve uploaded avatars as static content**

Expose the upload directory through Spring resource handling so the browser can load the image by URL.

---

### Task 3: Build a Facebook-style profile card in Settings

**Files:**
- Modify: `src/main/resources/templates/settings.html`
- Modify: `src/main/resources/static/js/banking.js` if helper functions are needed
- Modify: `src/main/resources/static/css/banking.css`

- [ ] **Step 1: Add a profile header block at the top of Settings**

Use a card layout with:
* circular avatar preview
* name, username, email, phone
* small status chips for tier / PIN / face
* upload button: `Tải ảnh từ máy`

- [ ] **Step 2: Add file input + preview**

Use a hidden `<input type="file" accept="image/*">` and show instant preview before upload.

- [ ] **Step 3: Add upload action**

On file pick, `POST /api/users/avatar` with `FormData`.

- [ ] **Step 4: Keep face settings separate**

Face registration continues to live in its own Settings tab and uses the existing face endpoints; do not merge it into profile avatar upload.

---

### Task 4: Show avatar across the app

**Files:**
- Modify: `src/main/resources/templates/fragments/layout.html`
- Modify: `src/main/resources/templates/dashboard.html`
- Modify: `src/main/resources/templates/settings.html`

- [ ] **Step 1: Show avatar in navbar**

Render a small circular avatar next to the user name in the navbar, falling back to initials if `avatarUrl` is empty.

- [ ] **Step 2: Show avatar in dashboard summary**

Add the same avatar to the dashboard header/profile area for consistency.

- [ ] **Step 3: Keep fallback behavior**

If no avatar exists, show a generated placeholder (initials or default icon) instead of a broken image.

---

### Task 5: Verification and testing

**Files:** none

- [ ] **Step 1: Test upload flow**

Upload a JPG/PNG from local machine, refresh the page, and confirm the avatar persists.

- [ ] **Step 2: Test fallback**

Remove the avatar or use a new account without one and confirm the placeholder appears.

- [ ] **Step 3: Confirm separation of concerns**

Face login and transfer verification should still use face endpoints/data, while profile avatar should only use `avatarUrl`.

---

## Files Modified
| File | Change |
|---|---|
| `src/main/java/com/moneytransfer/user/User.java` | Add `avatarUrl` field |
| `src/main/java/com/moneytransfer/user/UserController.java` | Expose avatar data, add upload endpoint |
| `src/main/java/com/moneytransfer/user/UserService.java` | Keep profile updates focused, no face/avatar conflation |
| `src/main/resources/templates/settings.html` | Add Facebook-style profile card and file upload UI |
| `src/main/resources/templates/fragments/layout.html` | Show avatar in navbar |
| `src/main/resources/templates/dashboard.html` | Show avatar in dashboard |
| `src/main/resources/static/css/banking.css` | Avatar/profile card styling |
