# Profile Avatar Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a separate profile avatar upload flow that stores an uploaded image on the server and shows it in Settings, navbar, and dashboard, while keeping face login / face verification logic unchanged and separate.

**Architecture:** Introduce a dedicated `avatarUrl` field on `User`, a multipart upload endpoint that saves image files into a server-owned `uploads/avatars/` directory, and a Spring resource handler that serves those files back over HTTP. The existing face pipeline stays on `faceImageUrl` and face-related endpoints only. The UI will render `avatarUrl` with a fallback placeholder when no avatar exists, and the Settings page will provide a Facebook-style profile card with upload/preview controls.

**Tech Stack:** Spring Boot, Spring MVC multipart upload, JPA, Thymeleaf, vanilla JS/CSS, MockMvc, Mockito/JUnit 5.

---

### Task 1: Add `avatarUrl` to the user model and profile payload

**Files:**
- Modify: `src/main/java/com/moneytransfer/user/User.java`
- Modify: `src/main/java/com/moneytransfer/user/UserController.java`
- Modify: `src/main/java/com/moneytransfer/user/UserService.java`
- Test: `src/test/java/com/moneytransfer/user/UserControllerTest.java` (create if missing)

- [ ] **Step 1: Write the failing controller test for `avatarUrl` in profile response**

Add a test that asserts `GET /api/users/profile` returns an `avatarUrl` field along with the existing fields.

```java
@Test
void getProfile_returnsAvatarUrl() {
    User user = new User();
    user.setUsername("user1");
    user.setAvatarUrl("/uploads/avatars/user1.png");
    when(userService.findByUsername("user1")).thenReturn(Optional.of(user));

    mockMvc.perform(get("/api/users/profile").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avatarUrl").value("/uploads/avatars/user1.png"));
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `mvn -Dtest=UserControllerTest test`

Expected: fail because `avatarUrl` is not yet returned.

- [ ] **Step 3: Add the `avatarUrl` field to `User`**

```java
@Column(length = 500)
private String avatarUrl;

public String getAvatarUrl() { return avatarUrl; }
public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
```

- [ ] **Step 4: Include `avatarUrl` in the profile response**

Return `avatarUrl` from the profile endpoint without changing face behavior:

```java
Map.entry("avatarUrl", user.getAvatarUrl()),
Map.entry("faceImageUrl", user.getFaceImageUrl()),
```

- [ ] **Step 5: Run the test and verify it passes**

Run: `mvn -Dtest=UserControllerTest test`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/moneytransfer/user/User.java src/main/java/com/moneytransfer/user/UserController.java src/test/java/com/moneytransfer/user/UserControllerTest.java
git commit -m "feat: add avatar url to profile payload"
```

---

### Task 2: Add server-side avatar upload and static file serving

**Files:**
- Create: `src/main/java/com/moneytransfer/config/AvatarStorageConfig.java`
- Modify: `src/main/java/com/moneytransfer/user/UserController.java`
- Test: `src/test/java/com/moneytransfer/user/UserControllerTest.java`

- [ ] **Step 1: Write the failing upload test**

Add a multipart test for `POST /api/users/avatar` that uploads a PNG and expects a URL to be saved and returned.

```java
@Test
void uploadAvatar_savesFileAndReturnsUrl() throws Exception {
    MockMultipartFile avatar = new MockMultipartFile(
            "avatar", "avatar.png", "image/png", new byte[] {1, 2, 3}
    );

    mockMvc.perform(multipart("/api/users/avatar").file(avatar).with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.avatarUrl").exists());
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `mvn -Dtest=UserControllerTest test`

Expected: fail because upload endpoint and static file serving do not exist yet.

- [ ] **Step 3: Implement `AvatarStorageConfig`**

Create a config class that exposes `uploads/avatars/` as a resource handler.

```java
@Configuration
public class AvatarStorageConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/avatars/**")
                .addResourceLocations("file:uploads/avatars/");
    }
}
```

- [ ] **Step 4: Implement `POST /api/users/avatar`**

Accept `MultipartFile avatar`, validate image type and size, save it to `uploads/avatars/`, and persist the relative URL into `avatarUrl`.

```java
if (avatar == null || avatar.isEmpty()) return ResponseEntity.badRequest().body(Map.of("error", "Avatar file is required"));
if (avatar.getContentType() == null || !avatar.getContentType().startsWith("image/")) return ResponseEntity.badRequest().body(Map.of("error", "Avatar must be an image"));
if (avatar.getSize() > 5 * 1024 * 1024) return ResponseEntity.badRequest().body(Map.of("error", "Avatar must be smaller than 5MB"));
```

Use a unique filename and save with `Files.copy(...)`.

- [ ] **Step 5: Run the test and verify it passes**

Run: `mvn -Dtest=UserControllerTest test`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/moneytransfer/config/AvatarStorageConfig.java src/main/java/com/moneytransfer/user/UserController.java src/test/java/com/moneytransfer/user/UserControllerTest.java
git commit -m "feat: add server-side avatar upload"
```

---

### Task 3: Build the Facebook-style profile card in Settings

**Files:**
- Modify: `src/main/resources/templates/settings.html`
- Modify: `src/main/resources/static/css/banking.css`
- Modify: `src/main/resources/static/js/banking.js`

- [ ] **Step 1: Add the profile card markup**

Replace the current plain personal-info block with a richer profile header card that includes:
* circular avatar preview
* name / username / email / phone
* upload button `Tải ảnh từ máy`
* a hidden file input with `accept="image/*"`

- [ ] **Step 2: Add CSS for the card and avatar**

Style the card with a prominent avatar circle, flexible row/column layout, and a clean upload button. Ensure it collapses well on mobile.

```css
.profile-card { display:flex; gap:20px; align-items:center; }
.profile-avatar { width:96px; height:96px; border-radius:50%; object-fit:cover; }
```

- [ ] **Step 3: Add the client-side upload flow**

Use `FormData` and `apiFetch` to upload the selected file to `/api/users/avatar`, then update the avatar preview immediately on success.

```javascript
function uploadAvatar(file) {
  var formData = new FormData();
  formData.append('avatar', file);
  return apiFetch('/api/users/avatar', { method: 'POST', body: formData });
}
```

- [ ] **Step 4: Keep face settings separate**

Do not move face login or face verification into this card. The face tab remains a separate settings area.

---

### Task 4: Show the avatar in navbar and dashboard

**Files:**
- Modify: `src/main/resources/templates/fragments/layout.html`
- Modify: `src/main/resources/templates/dashboard.html`

- [ ] **Step 1: Add avatar display in the navbar**

Render a small circular image next to the username in the top bar. If `avatarUrl` is empty, show initials or a default placeholder.

- [ ] **Step 2: Add avatar display to the dashboard header area**

Show the same avatar in the dashboard profile area so the visual identity is consistent across the app.

- [ ] **Step 3: Keep the fallback stable**

Use a placeholder `div` or initials-based fallback when no avatar exists; do not render a broken `<img>`.

---

### Task 5: Verify and harden

**Files:** none

- [ ] **Step 1: Verify upload persistence**

Upload a JPG/PNG, refresh the page, and confirm the image stays visible in Settings, navbar, and dashboard.

- [ ] **Step 2: Verify size and type restrictions**

Try a non-image file and a file larger than 5MB, confirm both are rejected with clear errors.

- [ ] **Step 3: Verify face separation remains intact**

Face login and transfer verification must still use the face-specific flow and not the profile avatar.

- [ ] **Step 4: Run the full test suite**

Run: `mvn test`

Expected: all tests pass.

---

## Files Modified
| File | Change |
|---|---|
| `src/main/java/com/moneytransfer/user/User.java` | Add `avatarUrl` |
| `src/main/java/com/moneytransfer/user/UserController.java` | Return avatar data and add upload endpoint |
| `src/main/java/com/moneytransfer/config/AvatarStorageConfig.java` | Serve uploaded avatars |
| `src/main/resources/templates/settings.html` | Facebook-style profile card + upload UI |
| `src/main/resources/templates/fragments/layout.html` | Avatar in navbar |
| `src/main/resources/templates/dashboard.html` | Avatar in dashboard |
| `src/main/resources/static/css/banking.css` | Avatar/profile styling |
| `src/main/resources/static/js/banking.js` | Upload preview / UI helpers if needed |
