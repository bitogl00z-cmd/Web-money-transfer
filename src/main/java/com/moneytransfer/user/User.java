package com.moneytransfer.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false, length = 255)
    private String passwordHash;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(columnDefinition = "TEXT")
    private String faceEncoding;

    @Column(length = 500)
    private String faceImageUrl;

    @Column(length = 500)
    private String avatarUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserTier tier = UserTier.STANDARD;

    @Column(nullable = false)
    private boolean otpEnabled = true;

    @Column(nullable = false)
    private boolean faceEnabled = false;

    @Column(nullable = false)
    private boolean locked = false;

    @Column(nullable = false)
    private int failedAttempts = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private boolean emailNotifications = true;

    @Column(name = "pin_hash", length = 255, nullable = true)
    private String pinHash;

    @Column(name = "pin_set", nullable = false)
    private Boolean pinSet = false;

    @Column(length = 5)
    private String language = "vi";

    public User() {}

    public User(String username, String passwordHash, String email, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public String getFaceEncoding() { return faceEncoding; }
    public void setFaceEncoding(String faceEncoding) { this.faceEncoding = faceEncoding; }
    public String getFaceImageUrl() { return faceImageUrl; }
    public void setFaceImageUrl(String faceImageUrl) { this.faceImageUrl = faceImageUrl; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public UserTier getTier() { return tier; }
    public void setTier(UserTier tier) { this.tier = tier; }
    public boolean isOtpEnabled() { return otpEnabled; }
    public void setOtpEnabled(boolean otpEnabled) { this.otpEnabled = otpEnabled; }
    public boolean isFaceEnabled() { return faceEnabled; }
    public void setFaceEnabled(boolean faceEnabled) { this.faceEnabled = faceEnabled; }
    public boolean isLocked() { return locked; }
    public void setLocked(boolean locked) { this.locked = locked; }
    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }
    public String getPinHash() { return pinHash; }
    public void setPinHash(String pinHash) { this.pinHash = pinHash; }
    public Boolean getPinSet() { return pinSet; }
    public void setPinSet(Boolean pinSet) { this.pinSet = pinSet; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
}
