# Money Transfer Application Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete money transfer web application with Spring Boot + Thymeleaf + MySQL + Redis, featuring JWT auth, face login, OTP, QR payments, recurring payments, admin dashboard, and export.

**Architecture:** Single Spring Boot monolith serving Thymeleaf views + REST API. JWT auth with Redis blacklist. Pessimistic locking with ordered lock acquisition for concurrency. Face recognition isolated on a dedicated thread pool.

**Tech Stack:** Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA, Thymeleaf, Alpine.js, Chart.js, Tailwind CSS, MySQL 8, Redis, OpenCV, ZXing, JavaMailSender, iText, Apache POI, MinIO, Maven

---

## File Structure Overview

```
money-transfer/
├── pom.xml
├── docker-compose.yml
├── src/
│   ├── main/
│   │   ├── java/com/moneytransfer/
│   │   │   ├── MoneyTransferApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   ├── StorageConfig.java
│   │   │   │   └── FaceProcessingConfig.java
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   └── TokenBlacklistService.java
│   │   │   ├── user/
│   │   │   │   ├── UserController.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── User.java
│   │   │   │   └── UserRepository.java
│   │   │   ├── account/
│   │   │   │   ├── AccountController.java
│   │   │   │   ├── AccountService.java
│   │   │   │   ├── Account.java
│   │   │   │   └── AccountRepository.java
│   │   │   ├── transaction/
│   │   │   │   ├── TransactionController.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   ├── Transaction.java
│   │   │   │   └── TransactionRepository.java
│   │   │   ├── otp/
│   │   │   │   ├── OtpController.java
│   │   │   │   ├── OtpService.java
│   │   │   │   ├── OtpCode.java
│   │   │   │   └── OtpRepository.java
│   │   │   ├── face/
│   │   │   │   ├── FaceController.java
│   │   │   │   ├── FaceService.java
│   │   │   │   └── FaceRecognitionUtil.java
│   │   │   ├── qr/
│   │   │   │   ├── QrController.java
│   │   │   │   └── QrService.java
│   │   │   ├── scheduled/
│   │   │   │   ├── ScheduledPaymentController.java
│   │   │   │   ├── ScheduledPaymentService.java
│   │   │   │   ├── ScheduledPayment.java
│   │   │   │   ├── ScheduledPaymentRepository.java
│   │   │   │   └── RecurringPaymentJob.java
│   │   │   ├── beneficiary/
│   │   │   │   ├── BeneficiaryController.java
│   │   │   │   ├── BeneficiaryService.java
│   │   │   │   ├── Beneficiary.java
│   │   │   │   └── BeneficiaryRepository.java
│   │   │   ├── admin/
│   │   │   │   ├── AdminController.java
│   │   │   │   └── AdminService.java
│   │   │   ├── dashboard/
│   │   │   │   ├── DashboardController.java
│   │   │   │   └── DashboardService.java
│   │   │   ├── export/
│   │   │   │   ├── ExportController.java
│   │   │   │   └── ExportService.java
│   │   │   ├── audit/
│   │   │   │   ├── AuditLog.java
│   │   │   │   ├── AuditLogRepository.java
│   │   │   │   └── AuditAspect.java
│   │   │   └── notification/
│   │   │       └── NotificationWebSocketHandler.java
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   ├── templates/
│   │   │   │   ├── layout.html
│   │   │   │   ├── login.html
│   │   │   │   ├── register.html
│   │   │   │   ├── dashboard.html
│   │   │   │   ├── transfer.html
│   │   │   │   ├── history.html
│   │   │   │   ├── beneficiaries.html
│   │   │   │   ├── scheduled.html
│   │   │   │   ├── profile.html
│   │   │   │   └── admin/
│   │   │   │       ├── users.html
│   │   │   │       └── transactions.html
│   │   │   └── static/
│   │   │       ├── css/
│   │   │       ├── js/
│   │   │       └── img/
│   └── test/
│       └── java/com/moneytransfer/
│           ├── auth/
│           ├── user/
│           ├── account/
│           ├── transaction/
│           ├── otp/
│           ├── face/
│           ├── qr/
│           ├── scheduled/
│           ├── beneficiary/
│           ├── admin/
│           ├── dashboard/
│           └── export/
```

---

### Task 1: Spring Boot project scaffolding + pom.xml

**Files:**
- Create: `money-transfer/pom.xml`
- Create: `money-transfer/src/main/java/com/moneytransfer/MoneyTransferApplication.java`
- Create: `money-transfer/src/main/resources/application.yml`

- [ ] **Step 1: Create pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.4</version>
    </parent>
    <groupId>com.moneytransfer</groupId>
    <artifactId>money-transfer</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>money-transfer</name>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-mail</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-websocket</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <!-- Thymeleaf extras for Spring Security -->
        <dependency>
            <groupId>org.thymeleaf.extras</groupId>
            <artifactId>thymeleaf-extras-springsecurity6</artifactId>
        </dependency>
        <dependency>
            <groupId>com.mysql</groupId>
            <artifactId>mysql-connector-j</artifactId>
            <scope>runtime</scope>
        </dependency>
        <!-- OpenCV -->
        <dependency>
            <groupId>org.openpnp</groupId>
            <artifactId>opencv</artifactId>
            <version>4.9.0-0</version>
        </dependency>
        <!-- ZXing for QR -->
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>core</artifactId>
            <version>3.5.3</version>
        </dependency>
        <dependency>
            <groupId>com.google.zxing</groupId>
            <artifactId>javase</artifactId>
            <version>3.5.3</version>
        </dependency>
        <!-- JWT -->
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.5</version>
            <scope>runtime</scope>
        </dependency>
        <!-- iText for PDF -->
        <dependency>
            <groupId>com.github.librepdf</groupId>
            <artifactId>openpdf</artifactId>
            <version>1.3.39</version>
        </dependency>
        <!-- Apache POI for Excel -->
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.6</version>
        </dependency>
        <!-- MinIO client -->
        <dependency>
            <groupId>io.minio</groupId>
            <artifactId>minio</artifactId>
            <version>8.5.10</version>
        </dependency>
        <!-- Test -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create MoneyTransferApplication.java**

```java
package com.moneytransfer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MoneyTransferApplication {
    public static void main(String[] args) {
        SpringApplication.run(MoneyTransferApplication.class, args);
    }
}
```

- [ ] **Step 3: Create application.yml**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/money_transfer?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    username: root
    password: root
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQLDialect
  thymeleaf:
    prefix: classpath:/templates/
    suffix: .html
    mode: HTML
    cache: false
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME:}
    password: ${MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB

app:
  jwt:
    secret: ${JWT_SECRET:a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2}
    access-token-expiration: 900000   # 15 minutes
    refresh-token-expiration: 604800000  # 7 days
  storage:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket: face-images
  face:
    max-image-size: 400
    thread-pool-size: 4
    similarity-threshold: 0.7
  checksum:
    secret-key: ${CHECKSUM_SECRET:money-transfer-checksum-secret-key-2026}

logging:
  level:
    com.moneytransfer: DEBUG
    org.springframework.security: INFO
```

- [ ] **Step 4: Verify project compiles**

Run: `mvn compile -q`
Expected: BUILD SUCCESS (no errors)

- [ ] **Step 5: Commit**

```bash
git init
git add pom.xml src/main/java/com/moneytransfer/MoneyTransferApplication.java src/main/resources/application.yml
git commit -m "chore: scaffold Spring Boot project"
```

---

### Task 2: Docker Compose (MySQL + Redis + MinIO)

**Files:**
- Create: `money-transfer/docker-compose.yml`

- [ ] **Step 1: Create docker-compose.yml**

```yaml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    container_name: money-transfer-mysql
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: money_transfer
    ports:
      - "3306:3306"
    volumes:
      - mysql-data:/var/lib/mysql

  redis:
    image: redis:7-alpine
    container_name: money-transfer-redis
    ports:
      - "6379:6379"

  minio:
    image: minio/minio:latest
    container_name: money-transfer-minio
    environment:
      MINIO_ROOT_USER: minioadmin
      MINIO_ROOT_PASSWORD: minioadmin
    command: server /data --console-address ":9001"
    ports:
      - "9000:9000"
      - "9001:9001"
    volumes:
      - minio-data:/data

volumes:
  mysql-data:
  minio-data:
```

- [ ] **Step 2: Start services**

Run: `docker compose up -d`
Expected: All 3 containers running (check with `docker ps`)

- [ ] **Step 3: Create MinIO bucket**

Run:
```bash
docker exec money-transfer-minio mc alias set local http://localhost:9000 minioadmin minioadmin
docker exec money-transfer-minio mc mb local/face-images
```
Expected: Bucket created successfully

- [ ] **Step 4: Commit**

```bash
git add docker-compose.yml
git commit -m "chore: add Docker Compose for MySQL, Redis, MinIO"
```

---

### Task 3: Entity classes (all JPA entities)

**Files:**
- Create: `src/main/java/com/moneytransfer/user/User.java`
- Create: `src/main/java/com/moneytransfer/account/Account.java`
- Create: `src/main/java/com/moneytransfer/transaction/Transaction.java`
- Create: `src/main/java/com/moneytransfer/beneficiary/Beneficiary.java`
- Create: `src/main/java/com/moneytransfer/scheduled/ScheduledPayment.java`
- Create: `src/main/java/com/moneytransfer/audit/AuditLog.java`
- Create: `src/main/java/com/moneytransfer/otp/OtpCode.java`

- [ ] **Step 1: Create User entity**

```java
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

    public User() {}

    public User(String username, String passwordHash, String email, String fullName) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.email = email;
        this.fullName = fullName;
    }

    // getters and setters
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
    public String getFaceEncoding() { return faceEncoding; }
    public void setFaceEncoding(String faceEncoding) { this.faceEncoding = faceEncoding; }
    public String getFaceImageUrl() { return faceImageUrl; }
    public void setFaceImageUrl(String faceImageUrl) { this.faceImageUrl = faceImageUrl; }
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
}
```

```java
package com.moneytransfer.user;

public enum UserTier {
    STANDARD, VIP
}
```

- [ ] **Step 2: Create Account entity**

```java
package com.moneytransfer.account;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(unique = true, nullable = false, length = 20)
    private String accountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 64)
    private String balanceChecksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Account() {}

    public Account(Long userId, String accountNumber) {
        this.userId = userId;
        this.accountNumber = accountNumber;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public BigDecimal getBalance() { return balance; }
    public void setBalance(BigDecimal balance) { this.balance = balance; }
    public String getBalanceChecksum() { return balanceChecksum; }
    public void setBalanceChecksum(String balanceChecksum) { this.balanceChecksum = balanceChecksum; }
    public AccountStatus getStatus() { return status; }
    public void setStatus(AccountStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

```java
package com.moneytransfer.account;

public enum AccountStatus {
    ACTIVE, FROZEN, CLOSED
}
```

- [ ] **Step 3: Create Transaction entity**

```java
package com.moneytransfer.transaction;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 20)
    private String transactionCode;

    @Column
    private Long fromAccountId;

    @Column
    private Long toAccountId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(precision = 15, scale = 2)
    private BigDecimal fromBalanceAfter;

    @Column(precision = 15, scale = 2)
    private BigDecimal toBalanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.COMPLETED;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Transaction() {}

    public Transaction(String transactionCode, Long fromAccountId, Long toAccountId,
                       BigDecimal amount, BigDecimal fromBalanceAfter, BigDecimal toBalanceAfter,
                       TransactionType type, String description) {
        this.transactionCode = transactionCode;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.fromBalanceAfter = fromBalanceAfter;
        this.toBalanceAfter = toBalanceAfter;
        this.type = type;
        this.description = description;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTransactionCode() { return transactionCode; }
    public void setTransactionCode(String transactionCode) { this.transactionCode = transactionCode; }
    public Long getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public void setToAccountId(Long toAccountId) { this.toAccountId = toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public BigDecimal getFromBalanceAfter() { return fromBalanceAfter; }
    public void setFromBalanceAfter(BigDecimal fromBalanceAfter) { this.fromBalanceAfter = fromBalanceAfter; }
    public BigDecimal getToBalanceAfter() { return toBalanceAfter; }
    public void setToBalanceAfter(BigDecimal toBalanceAfter) { this.toBalanceAfter = toBalanceAfter; }
    public TransactionType getType() { return type; }
    public void setType(TransactionType type) { this.type = type; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

```java
package com.moneytransfer.transaction;

public enum TransactionType {
    TRANSFER, WITHDRAW, DEPOSIT, RECURRING
}
```

```java
package com.moneytransfer.transaction;

public enum TransactionStatus {
    PENDING, COMPLETED, FAILED
}
```

- [ ] **Step 4: Create Beneficiary entity**

```java
package com.moneytransfer.beneficiary;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries")
public class Beneficiary {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String accountNumber;

    @Column(length = 100)
    private String nickname;

    @Column(length = 100)
    private String bankName = "INTERNAL";

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public Beneficiary() {}

    public Beneficiary(Long userId, String accountNumber, String nickname) {
        this.userId = userId;
        this.accountNumber = accountNumber;
        this.nickname = nickname;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getBankName() { return bankName; }
    public void setBankName(String bankName) { this.bankName = bankName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 5: Create ScheduledPayment entity**

```java
package com.moneytransfer.scheduled;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_payments")
public class ScheduledPayment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long fromAccountId;

    @Column(nullable = false, length = 20)
    private String toAccountNumber;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentFrequency frequency;

    @Column(nullable = false)
    private LocalDate nextRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public ScheduledPayment() {}

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFromAccountId() { return fromAccountId; }
    public void setFromAccountId(Long fromAccountId) { this.fromAccountId = fromAccountId; }
    public String getToAccountNumber() { return toAccountNumber; }
    public void setToAccountNumber(String toAccountNumber) { this.toAccountNumber = toAccountNumber; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public PaymentFrequency getFrequency() { return frequency; }
    public void setFrequency(PaymentFrequency frequency) { this.frequency = frequency; }
    public LocalDate getNextRun() { return nextRun; }
    public void setNextRun(LocalDate nextRun) { this.nextRun = nextRun; }
    public PaymentStatus getStatus() { return status; }
    public void setStatus(PaymentStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

```java
package com.moneytransfer.scheduled;

public enum PaymentFrequency {
    WEEKLY, MONTHLY
}
```

```java
package com.moneytransfer.scheduled;

public enum PaymentStatus {
    ACTIVE, PAUSED, CANCELLED
}
```

- [ ] **Step 6: Create OtpCode entity**

```java
package com.moneytransfer.otp;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
public class OtpCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 6)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpType type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean used = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public OtpCode() {}

    public OtpCode(Long userId, String code, OtpType type, LocalDateTime expiresAt) {
        this.userId = userId;
        this.code = code;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public OtpType getType() { return type; }
    public void setType(OtpType type) { this.type = type; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

```java
package com.moneytransfer.otp;

public enum OtpType {
    LOGIN, TRANSACTION
}
```

- [ ] **Step 7: Create AuditLog entity**

```java
package com.moneytransfer.audit;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(length = 45)
    private String ipAddress;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AuditLog() {}

    public AuditLog(Long userId, String action, String details, String ipAddress) {
        this.userId = userId;
        this.action = action;
        this.details = details;
        this.ipAddress = ipAddress;
    }

    // getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
```

- [ ] **Step 8: Run compilation check**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 9: Commit**

```bash
git add src/main/java/com/moneytransfer/user/ src/main/java/com/moneytransfer/account/ src/main/java/com/moneytransfer/transaction/ src/main/java/com/moneytransfer/beneficiary/ src/main/java/com/moneytransfer/scheduled/ src/main/java/com/moneytransfer/audit/ src/main/java/com/moneytransfer/otp/
git commit -m "feat: add all JPA entities"
```

---

### Task 4: JWT utility + Token blacklist (Redis)

**Files:**
- Create: `src/main/java/com/moneytransfer/auth/JwtUtil.java`
- Create: `src/main/java/com/moneytransfer/auth/TokenBlacklistService.java`
- Create: `src/main/java/com/moneytransfer/config/RedisConfig.java`

- [ ] **Step 1: Create JwtUtil.java**

```java
package com.moneytransfer.auth;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private final SecretKey secretKey;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtUtil(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration}") long accessExpiration,
            @Value("${app.jwt.refresh-token-expiration}") long refreshExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = accessExpiration;
        this.refreshExpiration = refreshExpiration;
    }

    public String generateAccessToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiration))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(Long userId, String username) {
        return Jwts.builder()
                .subject(username)
                .claim("userId", userId)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenExpired(String token) {
        try {
            Claims claims = validateToken(token);
            return claims.getExpiration().before(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return true;
        }
    }

    public long getExpirationMillis(String token) {
        Claims claims = validateToken(token);
        return claims.getExpiration().getTime() - System.currentTimeMillis();
    }
}
```

- [ ] **Step 2: Create RedisConfig.java**

```java
package com.moneytransfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        return template;
    }
}
```

- [ ] **Step 3: Create TokenBlacklistService.java**

```java
package com.moneytransfer.auth;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenBlacklistService {
    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String token, long ttlMillis) {
        String key = BLACKLIST_PREFIX + token;
        redisTemplate.opsForValue().set(key, "blacklisted", ttlMillis, TimeUnit.MILLISECONDS);
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }
}
```

- [ ] **Step 4: Run tests**

Write test to verify JWT generation and validation:

```java
package com.moneytransfer.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {
    private JwtUtil jwtUtil = new JwtUtil("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2", 900000, 604800000);

    @Test
    void testGenerateAndValidateToken() {
        String token = jwtUtil.generateAccessToken(1L, "testuser");
        assertNotNull(token);
        var claims = jwtUtil.validateToken(token);
        assertEquals("testuser", claims.getSubject());
        assertEquals(1, (int) claims.get("userId"));
    }

    @Test
    void testExpiredToken() {
        JwtUtil shortJwt = new JwtUtil("a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2", 1, 1);
        try { Thread.sleep(10); } catch (InterruptedException e) {}
        String token = shortJwt.generateAccessToken(1L, "test");
        assertTrue(shortJwt.isTokenExpired(token));
    }
}
```

Run: `mvn test -pl . -Dtest=JwtUtilTest -q`
Expected: Tests pass

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/moneytransfer/auth/JwtUtil.java src/main/java/com/moneytransfer/auth/TokenBlacklistService.java src/main/java/com/moneytransfer/config/RedisConfig.java
git commit -m "feat: add JWT util and Redis token blacklist"
```

---

### Task 5: Security configuration + JWT auth filter

**Files:**
- Create: `src/main/java/com/moneytransfer/config/SecurityConfig.java`
- Create: `src/main/java/com/moneytransfer/auth/JwtAuthFilter.java`

- [ ] **Step 1: Create JwtAuthFilter.java**

```java
package com.moneytransfer.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService blacklistService;

    public JwtAuthFilter(JwtUtil jwtUtil, TokenBlacklistService blacklistService) {
        this.jwtUtil = jwtUtil;
        this.blacklistService = blacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (token != null) {
            if (blacklistService.isBlacklisted(token)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
            try {
                Claims claims = jwtUtil.validateToken(token);
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(claims.getSubject(), null, Collections.emptyList());
                auth.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // Also check cookie
        if (request.getCookies() != null) {
            for (var cookie : request.getCookies()) {
                if ("access_token".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: Create SecurityConfig.java**

```java
package com.moneytransfer.config;

import com.moneytransfer.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/otp/**", "/api/face/**").permitAll()
                .requestMatchers("/css/**", "/js/**", "/img/**").permitAll()
                .requestMatchers("/login", "/register").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .formLogin(form -> form
                .loginPage("/login")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login")
                .permitAll()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
```

- [ ] **Step 3: Compile check**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/moneytransfer/config/SecurityConfig.java src/main/java/com/moneytransfer/auth/JwtAuthFilter.java
git commit -m "feat: add security config and JWT auth filter"
```

---

### Task 6: Repositories layer

**Files:**
- Create: `src/main/java/com/moneytransfer/user/UserRepository.java`
- Create: `src/main/java/com/moneytransfer/account/AccountRepository.java`
- Create: `src/main/java/com/moneytransfer/transaction/TransactionRepository.java`
- Create: `src/main/java/com/moneytransfer/beneficiary/BeneficiaryRepository.java`
- Create: `src/main/java/com/moneytransfer/scheduled/ScheduledPaymentRepository.java`
- Create: `src/main/java/com/moneytransfer/audit/AuditLogRepository.java`
- Create: `src/main/java/com/moneytransfer/otp/OtpRepository.java`

- [ ] **Step 1: Create all repositories**

```java
package com.moneytransfer.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

```java
package com.moneytransfer.account;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByUserId(Long userId);
    Optional<Account> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIdWithLock(@Param("id") Long id);
}
```

```java
package com.moneytransfer.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(
            Long fromAccountId, Long toAccountId, Pageable pageable);

    List<Transaction> findByFromAccountIdOrToAccountIdAndCreatedAtBetween(
            Long fromAccountId, Long toAccountId, LocalDateTime from, LocalDateTime to);
}
```

```java
package com.moneytransfer.beneficiary;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BeneficiaryRepository extends JpaRepository<Beneficiary, Long> {
    List<Beneficiary> findByUserId(Long userId);
}
```

```java
package com.moneytransfer.scheduled;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {
    List<ScheduledPayment> findByUserId(Long userId);
    List<ScheduledPayment> findByStatusAndNextRunLessThanEqual(ScheduledPayment.PaymentStatus status, LocalDate date);
    List<ScheduledPayment> findByStatus(ScheduledPayment.PaymentStatus status);
}
```

Wait — the ScheduledPayment entity does not have a `userId` field. Let me fix: it has `fromAccountId`. Instead:
```java
package com.moneytransfer.scheduled;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ScheduledPaymentRepository extends JpaRepository<ScheduledPayment, Long> {
    List<ScheduledPayment> findByFromAccountId(Long fromAccountId);
    List<ScheduledPayment> findByStatusAndNextRunLessThanEqual(PaymentStatus status, LocalDate date);
}
```

```java
package com.moneytransfer.audit;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
```

```java
package com.moneytransfer.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OtpRepository extends JpaRepository<OtpCode, Long> {
    Optional<OtpCode> findByUserIdAndCodeAndTypeAndUsedFalseOrderByCreatedAtDesc(
            Long userId, String code, OtpCode.OtpType type);
}
```

- [ ] **Step 2: Compile check**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/user/UserRepository.java src/main/java/com/moneytransfer/account/AccountRepository.java src/main/java/com/moneytransfer/transaction/TransactionRepository.java src/main/java/com/moneytransfer/beneficiary/BeneficiaryRepository.java src/main/java/com/moneytransfer/scheduled/ScheduledPaymentRepository.java src/main/java/com/moneytransfer/audit/AuditLogRepository.java src/main/java/com/moneytransfer/otp/OtpRepository.java
git commit -m "feat: add all JPA repositories"
```

---

### Task 7: Auth service + controller (register, login, logout)

**Files:**
- Create: `src/main/java/com/moneytransfer/auth/AuthService.java`
- Create: `src/main/java/com/moneytransfer/auth/AuthController.java`
- Create: `src/main/java/com/moneytransfer/user/UserService.java`
- Create: `src/main/java/com/moneytransfer/user/UserController.java`

- [ ] **Step 1: Create AuthService.java**

```java
package com.moneytransfer.auth;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.account.AccountStatus;
import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final AccountService accountService;
    private final AuditLogRepository auditLogRepository;

    public TransactionService(AccountRepository accountRepository,
                              TransactionRepository transactionRepository,
                              AccountService accountService,
                              AuditLogRepository auditLogRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.accountService = accountService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public Transaction transfer(Long fromAccountId, Long toAccountId, BigDecimal amount, String description, Long userId, String ip) {
        // Deadlock prevention: lock smaller ID first
        Long firstId = Math.min(fromAccountId, toAccountId);
        Long secondId = Math.max(fromAccountId, toAccountId);

        Account first = accountRepository.findByIdWithLock(firstId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + firstId));
        Account second = accountRepository.findByIdWithLock(secondId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + secondId));

        // Verify integrity checksums
        accountService.verifyChecksum(first);
        accountService.verifyChecksum(second);

        Account from = first.getId().equals(fromAccountId) ? first : second;
        Account to = first.getId().equals(fromAccountId) ? second : first;

        if (from.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Sender account is not active");
        }
        if (to.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalStateException("Receiver account is not active");
        }
        if (from.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));

        from.setBalanceChecksum(accountService.computeChecksum(from));
        to.setBalanceChecksum(accountService.computeChecksum(to));

        accountRepository.save(from);
        accountRepository.save(to);

        String txCode = generateTransactionCode();
        Transaction tx = new Transaction(txCode, fromAccountId, toAccountId, amount,
                from.getBalance(), to.getBalance(), TransactionType.TRANSFER, description);
        tx = transactionRepository.save(tx);

        auditLogRepository.save(new AuditLog(userId, "TRANSFER",
                "Transferred " + amount + " from " + fromAccountId + " to " + toAccountId, ip));

        return tx;
    }

    @Transactional
    public Transaction deposit(Long accountId, BigDecimal amount, String description, Long userId, String ip) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        accountService.verifyChecksum(account);

        account.setBalance(account.getBalance().add(amount));
        account.setBalanceChecksum(accountService.computeChecksum(account));
        accountRepository.save(account);

        String txCode = generateTransactionCode();
        Transaction tx = new Transaction(txCode, null, accountId, amount,
                null, account.getBalance(), TransactionType.DEPOSIT, description);
        tx = transactionRepository.save(tx);

        auditLogRepository.save(new AuditLog(userId, "DEPOSIT",
                "Deposited " + amount + " to account " + accountId, ip));
        return tx;
    }

    @Transactional
    public Transaction withdraw(Long accountId, BigDecimal amount, String description, Long userId, String ip) {
        Account account = accountRepository.findByIdWithLock(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
        accountService.verifyChecksum(account);

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(amount));
        account.setBalanceChecksum(accountService.computeChecksum(account));
        accountRepository.save(account);

        String txCode = generateTransactionCode();
        Transaction tx = new Transaction(txCode, accountId, null, amount,
                account.getBalance(), null, TransactionType.WITHDRAW, description);
        tx = transactionRepository.save(tx);

        auditLogRepository.save(new AuditLog(userId, "WITHDRAW",
                "Withdrew " + amount + " from account " + accountId, ip));
        return tx;
    }

    public Page<Transaction> getHistory(Long accountId, Pageable pageable) {
        return transactionRepository.findByFromAccountIdOrToAccountIdOrderByCreatedAtDesc(accountId, accountId, pageable);
    }

    public List<Transaction> getHistoryBetween(Long accountId, LocalDateTime from, LocalDateTime to) {
        return transactionRepository.findByFromAccountIdOrToAccountIdAndCreatedAtBetween(accountId, accountId, from, to);
    }

    private String generateTransactionCode() {
        String datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        Random random = new Random();
        StringBuilder sb = new StringBuilder("TX").append(datePart);
        for (int i = 0; i < 6; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }
}
```

- [ ] **Step 2: Create TransactionController.java**

```java
package com.moneytransfer.transaction;

import com.moneytransfer.auth.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {
    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

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
            String ip = request.getRemoteAddr();
            Transaction tx = transactionService.transfer(fromId, toId, amount, description, userId, ip);
            return ResponseEntity.ok(Map.of("transactionCode", tx.getTransactionCode(), "status", tx.getStatus()));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody Map<String, Object> body,
                                      Authentication auth, HttpServletRequest request) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long userId = ((Integer) claims.get("userId")).longValue();
            Long accountId = Long.valueOf(body.get("accountId").toString());
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String ip = request.getRemoteAddr();
            Transaction tx = transactionService.deposit(accountId, amount, "Deposit", userId, ip);
            return ResponseEntity.ok(Map.of("transactionCode", tx.getTransactionCode(), "status", tx.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody Map<String, Object> body,
                                       Authentication auth, HttpServletRequest request) {
        try {
            Claims claims = (Claims) auth.getDetails();
            Long userId = ((Integer) claims.get("userId")).longValue();
            Long accountId = Long.valueOf(body.get("accountId").toString());
            BigDecimal amount = new BigDecimal(body.get("amount").toString());
            String ip = request.getRemoteAddr();
            Transaction tx = transactionService.withdraw(accountId, amount, "Withdrawal", userId, ip);
            return ResponseEntity.ok(Map.of("transactionCode", tx.getTransactionCode(), "status", tx.getStatus()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(Authentication auth,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        Page<Transaction> history = transactionService.getHistory(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTransaction(@PathVariable Long id) {
        return transactionService.findById(id)
                .map(tx -> ResponseEntity.ok(Map.of(
                        "transactionCode", tx.getTransactionCode(),
                        "amount", tx.getAmount(),
                        "type", tx.getType(),
                        "status", tx.getStatus(),
                        "createdAt", tx.getCreatedAt()
                )))
                .orElse(ResponseEntity.notFound().build());
    }
}
```

Add `findById` to TransactionService:
```java
    public java.util.Optional<Transaction> findById(Long id) {
        return transactionRepository.findById(id);
    }
```

- [ ] **Step 3: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/moneytransfer/transaction/TransactionService.java src/main/java/com/moneytransfer/transaction/TransactionController.java
git commit -m "feat: implement transaction service with pessimistic locking and integrity checks"
```

---

### Task 10: Transaction tests

**Files:**
- Create: `src/test/java/com/moneytransfer/transaction/TransactionServiceTest.java`

- [ ] **Step 1: Write transaction test**

```java
package com.moneytransfer.transaction;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.audit.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    @Mock private AccountRepository accountRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountService accountService;
    @Mock private AuditLogRepository auditLogRepository;

    private TransactionService transactionService;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionService(accountRepository, transactionRepository,
                accountService, auditLogRepository);
    }

    @Test
    void testTransfer_Success() {
        Account from = new Account(1L, "MT00000000000001");
        from.setId(1L);
        from.setBalance(new BigDecimal("1000.00"));
        from.setStatus(Account.AccountStatus.ACTIVE);

        Account to = new Account(2L, "MT00000000000002");
        to.setId(2L);
        to.setBalance(new BigDecimal("500.00"));
        to.setStatus(Account.AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(to));
        when(accountService.computeChecksum(any())).thenReturn("checksum");
        Transaction savedTx = new Transaction();
        savedTx.setTransactionCode("TX20260524000001");
        when(transactionRepository.save(any())).thenReturn(savedTx);

        Transaction result = transactionService.transfer(1L, 2L, new BigDecimal("200.00"), "test", 1L, "127.0.0.1");

        assertNotNull(result);
        assertEquals(new BigDecimal("800.00"), from.getBalance());
        assertEquals(new BigDecimal("700.00"), to.getBalance());
        verify(accountRepository, times(2)).save(any());
    }

    @Test
    void testTransfer_InsufficientBalance() {
        Account from = new Account(1L, "MT00000000000001");
        from.setId(1L);
        from.setBalance(new BigDecimal("100.00"));
        from.setStatus(Account.AccountStatus.ACTIVE);

        Account to = new Account(2L, "MT00000000000002");
        to.setId(2L);
        to.setBalance(new BigDecimal("500.00"));
        to.setStatus(Account.AccountStatus.ACTIVE);

        when(accountRepository.findByIdWithLock(1L)).thenReturn(Optional.of(from));
        when(accountRepository.findByIdWithLock(2L)).thenReturn(Optional.of(to));

        assertThrows(IllegalArgumentException.class, () ->
                transactionService.transfer(1L, 2L, new BigDecimal("200.00"), "test", 1L, "127.0.0.1"));
    }
}
```

- [ ] **Step 2: Run test**

Run: `mvn test -Dtest=TransactionServiceTest -q`
Expected: Tests pass

- [ ] **Step 3: Commit**

```bash
git add src/test/java/com/moneytransfer/transaction/TransactionServiceTest.java
git commit -m "test: add transaction service unit tests"
```

---

### Task 11: OTP module

**Files:**
- Create: `src/main/java/com/moneytransfer/otp/OtpService.java`
- Create: `src/main/java/com/moneytransfer/otp/OtpController.java`

- [ ] **Step 1: Create OtpService.java**

```java
package com.moneytransfer.otp;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {
    private final OtpRepository otpRepository;
    private final JavaMailSender mailSender;

    public OtpService(OtpRepository otpRepository, JavaMailSender mailSender) {
        this.otpRepository = otpRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public String generateAndSendOtp(Long userId, String email, OtpType type) {
        String code = String.format("%06d", new Random().nextInt(999999));
        OtpCode otp = new OtpCode(userId, code, type, LocalDateTime.now().plusMinutes(5));
        otpRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("Money Transfer - OTP Code");
        message.setText("Your OTP code is: " + code + "\nExpires in 5 minutes.");
        mailSender.send(message);

        return code;
    }

    public boolean verifyOtp(Long userId, String code, OtpType type) {
        return otpRepository.findByUserIdAndCodeAndTypeAndUsedFalseOrderByCreatedAtDesc(userId, code, type)
                .map(otp -> {
                    if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
                        return false;
                    }
                    otp.setUsed(true);
                    otpRepository.save(otp);
                    return true;
                })
                .orElse(false);
    }
}
```

- [ ] **Step 2: Create OtpController.java**

```java
package com.moneytransfer.otp;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/otp")
public class OtpController {
    private final OtpService otpService;
    private final UserRepository userRepository;

    public OtpController(OtpService otpService, UserRepository userRepository) {
        this.otpService = otpService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<?> sendOtp(Authentication auth, @RequestBody Map<String, String> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        User user = userRepository.findById(userId).orElseThrow();
        OtpType type = OtpType.valueOf(body.getOrDefault("type", "LOGIN"));
        otpService.generateAndSendOtp(userId, user.getEmail(), type);
        return ResponseEntity.ok(Map.of("message", "OTP sent to " + user.getEmail()));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(Authentication auth, @RequestBody Map<String, String> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        String code = body.get("code");
        OtpType type = OtpType.valueOf(body.getOrDefault("type", "LOGIN"));
        boolean valid = otpService.verifyOtp(userId, code, type);
        if (valid) {
            return ResponseEntity.ok(Map.of("message", "OTP verified"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid or expired OTP"));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/otp/OtpService.java src/main/java/com/moneytransfer/otp/OtpController.java
git commit -m "feat: implement OTP send via email and verify"
```

---

### Task 12: Face module (OpenCV + async ThreadPool)

**Files:**
- Create: `src/main/java/com/moneytransfer/config/FaceProcessingConfig.java`
- Create: `src/main/java/com/moneytransfer/face/FaceRecognitionUtil.java`
- Create: `src/main/java/com/moneytransfer/face/FaceService.java`
- Create: `src/main/java/com/moneytransfer/face/FaceController.java`

- [ ] **Step 1: Create FaceProcessingConfig.java**

```java
package com.moneytransfer.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Configuration
public class FaceProcessingConfig {
    @Bean("faceExecutor")
    public Executor faceExecutor(@Value("${app.face.thread-pool-size:4}") int poolSize) {
        return Executors.newFixedThreadPool(poolSize);
    }
}
```

- [ ] **Step 2: Create FaceRecognitionUtil.java**

```java
package com.moneytransfer.face;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@Component
public class FaceRecognitionUtil {
    private final CascadeClassifier faceDetector;
    private final int maxImageSize;

    static { System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

    public FaceRecognitionUtil(@Value("${app.face.max-image-size:400}") int maxImageSize) {
        this.maxImageSize = maxImageSize;
        this.faceDetector = new CascadeClassifier(
                "haarcascade_frontalface_default.xml");
    }

    public Mat resizeToMax(Mat src) {
        int maxDim = Math.max(src.width(), src.height());
        if (maxDim <= maxImageSize) return src;
        double scale = (double) maxImageSize / maxDim;
        Mat resized = new Mat();
        Imgproc.resize(src, resized, new Size(), scale, scale);
        return resized;
    }

    public Rect detectFace(Mat image) {
        Mat gray = new Mat();
        Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY);
        MatOfRect faces = new MatOfRect();
        faceDetector.detectMultiScale(gray, faces);
        Rect[] rects = faces.toArray();
        if (rects.length == 0) throw new RuntimeException("No face detected");
        return rects[0];
    }

    public double[] computeEncoding(Mat image, Rect faceRect) {
        Mat face = new Mat(image, faceRect);
        Mat gray = new Mat();
        Imgproc.cvtColor(face, gray, Imgproc.COLOR_BGR2GRAY);
        Mat resized = new Mat();
        Imgproc.resize(gray, resized, new Size(128, 128));
        // Flatten pixel values as simple encoding (LBP-like histogram approach)
        double[] encoding = new double[128 * 128];
        resized.get(0, 0, encoding);
        return normalize(encoding);
    }

    public double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private double[] normalize(double[] vec) {
        double norm = 0;
        for (double v : vec) norm += v * v;
        norm = Math.sqrt(norm);
        if (norm == 0) return vec;
        for (int i = 0; i < vec.length; i++) vec[i] /= norm;
        return vec;
    }
}
```

- [ ] **Step 3: Create FaceService.java**

```java
package com.moneytransfer.face;

import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class FaceService {
    private final FaceRecognitionUtil faceUtil;
    private final UserRepository userRepository;
    private final Executor faceExecutor;
    private final double similarityThreshold;

    public FaceService(FaceRecognitionUtil faceUtil, UserRepository userRepository,
                       @Qualifier("faceExecutor") Executor faceExecutor,
                       @Value("${app.face.similarity-threshold:0.7}") double similarityThreshold) {
        this.faceUtil = faceUtil;
        this.userRepository = userRepository;
        this.faceExecutor = faceExecutor;
        this.similarityThreshold = similarityThreshold;
    }

    public CompletableFuture<String> registerFace(Long userId, MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            try {
                MatOfByte matOfByte = new MatOfByte(file.getBytes());
                Mat image = faceUtil.resizeToMax(Imgcodecs.imdecode(matOfByte, Imgcodecs.IMREAD_COLOR));
                Rect faceRect = faceUtil.detectFace(image);
                double[] encoding = faceUtil.computeEncoding(image, faceRect);
                String encoded = Arrays.stream(encoding)
                        .mapToObj(String::valueOf)
                        .collect(Collectors.joining(","));
                user.setFaceEncoding(encoded);
                user.setFaceEnabled(true);
                userRepository.save(user);
                return "Face registered successfully";
            } catch (Exception e) {
                throw new RuntimeException("Face registration failed: " + e.getMessage());
            }
        }, faceExecutor);
    }

    public CompletableFuture<Boolean> verifyFace(Long userId, MultipartFile file) {
        return CompletableFuture.supplyAsync(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (user.getFaceEncoding() == null) {
                throw new IllegalArgumentException("No face registered");
            }
            try {
                MatOfByte matOfByte = new MatOfByte(file.getBytes());
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
}
```

- [ ] **Step 4: Create FaceController.java**

```java
package com.moneytransfer.face;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/face")
public class FaceController {
    private final FaceService faceService;

    public FaceController(FaceService faceService) {
        this.faceService = faceService;
    }

    @PostMapping("/register")
    public CompletableFuture<ResponseEntity<?>> registerFace(
            Authentication auth, @RequestParam("file") MultipartFile file) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return faceService.registerFace(userId, file)
                .thenApply(msg -> ResponseEntity.ok(Map.of("message", msg)))
                .exceptionally(e -> ResponseEntity.badRequest()
                        .body(Map.of("error", e.getCause().getMessage())));
    }

    @PostMapping("/login")
    public CompletableFuture<ResponseEntity<?>> verifyFace(
            Authentication auth, @RequestParam("file") MultipartFile file) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return faceService.verifyFace(userId, file)
                .thenApply(match -> ResponseEntity.ok(Map.of("matched", match)))
                .exceptionally(e -> ResponseEntity.badRequest()
                        .body(Map.of("error", e.getCause().getMessage())));
    }
}
```

- [ ] **Step 5: Compile**

Run: `mvn compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/moneytransfer/config/FaceProcessingConfig.java src/main/java/com/moneytransfer/face/
git commit -m "feat: implement face recognition with async thread pool"
```

---

### Task 13: QR code module

**Files:**
- Create: `src/main/java/com/moneytransfer/qr/QrService.java`
- Create: `src/main/java/com/moneytransfer/qr/QrController.java`

- [ ] **Step 1: Create QrService.java**

```java
package com.moneytransfer.qr;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class QrService {
    public byte[] generateQr(String data, int width, int height) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(data, BarcodeFormat.QR_CODE, width, height);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(matrix, "PNG", out);
        return out.toByteArray();
    }
}
```

- [ ] **Step 2: Create QrController.java**

```java
package com.moneytransfer.qr;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/qr")
public class QrController {
    private final QrService qrService;
    private final AccountRepository accountRepository;

    public QrController(QrService qrService, AccountRepository accountRepository) {
        this.qrService = qrService;
        this.accountRepository = accountRepository;
    }

    @GetMapping(value = "/generate", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> generateQr(@RequestParam Long accountId) throws Exception {
        Account account = accountRepository.findById(accountId).orElseThrow();
        String qrData = "MT://pay?account=" + account.getAccountNumber();
        byte[] qrImage = qrService.generateQr(qrData, 300, 300);
        return ResponseEntity.ok(qrImage);
    }

    @PostMapping("/scan")
    public ResponseEntity<?> scanQr(@RequestBody Map<String, String> body) {
        String qrData = body.get("data");
        String accountNumber = qrData.replace("MT://pay?account=", "");
        return ResponseEntity.ok(Map.of("accountNumber", accountNumber));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/qr/
git commit -m "feat: implement QR generate and scan"
```

---

### Task 14: Beneficiaries module

**Files:**
- Create: `src/main/java/com/moneytransfer/beneficiary/BeneficiaryService.java`
- Create: `src/main/java/com/moneytransfer/beneficiary/BeneficiaryController.java`

- [ ] **Step 1: Create BeneficiaryService.java**

```java
package com.moneytransfer.beneficiary;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeneficiaryService {
    private final BeneficiaryRepository beneficiaryRepository;

    public BeneficiaryService(BeneficiaryRepository beneficiaryRepository) {
        this.beneficiaryRepository = beneficiaryRepository;
    }

    public List<Beneficiary> getUserBeneficiaries(Long userId) {
        return beneficiaryRepository.findByUserId(userId);
    }

    @Transactional
    public Beneficiary addBeneficiary(Long userId, String accountNumber, String nickname) {
        Beneficiary beneficiary = new Beneficiary(userId, accountNumber, nickname);
        return beneficiaryRepository.save(beneficiary);
    }

    @Transactional
    public void deleteBeneficiary(Long id, Long userId) {
        Beneficiary beneficiary = beneficiaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Beneficiary not found"));
        if (!beneficiary.getUserId().equals(userId)) {
            throw new SecurityException("Not authorized");
        }
        beneficiaryRepository.delete(beneficiary);
    }
}
```

- [ ] **Step 2: Create BeneficiaryController.java**

```java
package com.moneytransfer.beneficiary;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users/beneficiaries")
public class BeneficiaryController {
    private final BeneficiaryService beneficiaryService;

    public BeneficiaryController(BeneficiaryService beneficiaryService) {
        this.beneficiaryService = beneficiaryService;
    }

    @GetMapping
    public ResponseEntity<?> getBeneficiaries(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return ResponseEntity.ok(beneficiaryService.getUserBeneficiaries(userId));
    }

    @PostMapping
    public ResponseEntity<?> addBeneficiary(Authentication auth, @RequestBody Map<String, String> body) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        try {
            Beneficiary beneficiary = beneficiaryService.addBeneficiary(userId,
                    body.get("accountNumber"), body.get("nickname"));
            return ResponseEntity.ok(Map.of("id", beneficiary.getId(), "message", "Beneficiary added"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBeneficiary(Authentication auth, @PathVariable Long id) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        try {
            beneficiaryService.deleteBeneficiary(id, userId);
            return ResponseEntity.ok(Map.of("message", "Beneficiary deleted"));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/beneficiary/BeneficiaryService.java src/main/java/com/moneytransfer/beneficiary/BeneficiaryController.java
git commit -m "feat: implement beneficiaries CRUD"
```

---

### Task 15: Scheduled payments + recurring job

**Files:**
- Create: `src/main/java/com/moneytransfer/scheduled/ScheduledPaymentService.java`
- Create: `src/main/java/com/moneytransfer/scheduled/ScheduledPaymentController.java`
- Create: `src/main/java/com/moneytransfer/scheduled/RecurringPaymentJob.java`

- [ ] **Step 1: Create ScheduledPaymentService.java**

```java
package com.moneytransfer.scheduled;

import com.moneytransfer.account.Account;
import com.moneytransfer.account.AccountRepository;
import com.moneytransfer.account.AccountService;
import com.moneytransfer.transaction.Transaction;
import com.moneytransfer.transaction.TransactionRepository;
import com.moneytransfer.transaction.TransactionType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Random;

@Service
public class ScheduledPaymentService {
    private final ScheduledPaymentRepository scheduledPaymentRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;
    private final TransactionRepository transactionRepository;

    public ScheduledPaymentService(ScheduledPaymentRepository scheduledPaymentRepository,
                                   AccountRepository accountRepository, AccountService accountService,
                                   TransactionRepository transactionRepository) {
        this.scheduledPaymentRepository = scheduledPaymentRepository;
        this.accountRepository = accountRepository;
        this.accountService = accountService;
        this.transactionRepository = transactionRepository;
    }

    @Transactional
    public ScheduledPayment create(ScheduledPayment payment) {
        return scheduledPaymentRepository.save(payment);
    }

    public List<ScheduledPayment> getUserPayments(Long fromAccountId) {
        return scheduledPaymentRepository.findByFromAccountId(fromAccountId);
    }

    @Transactional
    public void cancel(Long id) {
        ScheduledPayment payment = scheduledPaymentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found"));
        payment.setStatus(PaymentStatus.CANCELLED);
        scheduledPaymentRepository.save(payment);
    }

    @Transactional
    public void processDuePayments() {
        List<ScheduledPayment> due = scheduledPaymentRepository
                .findByStatusAndNextRunLessThanEqual(PaymentStatus.ACTIVE, LocalDate.now());
        for (ScheduledPayment payment : due) {
            try {
                processPayment(payment);
                payment.setNextRun(computeNextRun(payment));
            } catch (Exception e) {
                payment.setStatus(PaymentStatus.PAUSED);
            }
            scheduledPaymentRepository.save(payment);
        }
    }

    private void processPayment(ScheduledPayment payment) {
        Account from = accountRepository.findByAccountNumber(
                payment.getToAccountNumber()).orElse(null);
        if (from == null) return;
        Account senderAccount = accountRepository.findByIdWithLock(payment.getFromAccountId())
                .orElseThrow();
        accountService.verifyChecksum(senderAccount);
        if (senderAccount.getBalance().compareTo(payment.getAmount()) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }
        senderAccount.setBalance(senderAccount.getBalance().subtract(payment.getAmount()));
        senderAccount.setBalanceChecksum(accountService.computeChecksum(senderAccount));
        accountRepository.save(senderAccount);
        String txCode = "TX" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + String.format("%06d", new Random().nextInt(999999));
        transactionRepository.save(new Transaction(txCode, payment.getFromAccountId(), from.getId(),
                payment.getAmount(), senderAccount.getBalance(), null, TransactionType.RECURRING,
                payment.getDescription()));
    }

    private LocalDate computeNextRun(ScheduledPayment payment) {
        return switch (payment.getFrequency()) {
            case WEEKLY -> payment.getNextRun().plusWeeks(1);
            case MONTHLY -> payment.getNextRun().plusMonths(1);
        };
    }
}
```

- [ ] **Step 2: Create RecurringPaymentJob.java**

```java
package com.moneytransfer.scheduled;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RecurringPaymentJob {
    private final ScheduledPaymentService scheduledPaymentService;

    public RecurringPaymentJob(ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    @Scheduled(cron = "0 0 8 * * *") // every day at 8:00 AM
    public void processRecurringPayments() {
        scheduledPaymentService.processDuePayments();
    }
}
```

- [ ] **Step 3: Create ScheduledPaymentController.java**

```java
package com.moneytransfer.scheduled;

import io.jsonwebtoken.Claims;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scheduled-payments")
public class ScheduledPaymentController {
    private final ScheduledPaymentService scheduledPaymentService;

    public ScheduledPaymentController(ScheduledPaymentService scheduledPaymentService) {
        this.scheduledPaymentService = scheduledPaymentService;
    }

    @GetMapping
    public List<ScheduledPayment> getUserPayments(Authentication auth) {
        Claims claims = (Claims) auth.getDetails();
        Long userId = ((Integer) claims.get("userId")).longValue();
        return scheduledPaymentService.getUserPayments(userId);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body) {
        ScheduledPayment payment = new ScheduledPayment();
        payment.setFromAccountId(Long.valueOf(body.get("fromAccountId").toString()));
        payment.setToAccountNumber(body.get("toAccountNumber").toString());
        payment.setAmount(new BigDecimal(body.get("amount").toString()));
        payment.setDescription((String) body.getOrDefault("description", ""));
        payment.setFrequency(PaymentFrequency.valueOf(body.get("frequency").toString()));
        payment.setNextRun(LocalDate.parse(body.get("nextRun").toString()));
        return ResponseEntity.ok(scheduledPaymentService.create(payment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> cancel(@PathVariable Long id) {
        scheduledPaymentService.cancel(id);
        return ResponseEntity.ok(Map.of("message", "Payment cancelled"));
    }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/moneytransfer/scheduled/
git commit -m "feat: implement scheduled payments and recurring job"
```

---

### Task 16: Audit log AOP aspect

**Files:**
- Create: `src/main/java/com/moneytransfer/audit/AuditAspect.java`

- [ ] **Step 1: Create AuditAspect.java**

```java
package com.moneytransfer.audit;

import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
public class AuditAspect {
    private final AuditLogRepository auditLogRepository;

    public AuditAspect(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @AfterReturning("@annotation(auditable)")
    public void logAudit(JoinPoint joinPoint, Auditable auditable) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes)
                    RequestContextHolder.getRequestAttributes();
            if (attributes == null) return;
            HttpServletRequest request = attributes.getRequest();

            Authentication auth = (Authentication) request.getUserPrincipal();
            if (auth == null) return;

            Long userId = ((Integer) ((io.jsonwebtoken.Claims) auth.getDetails()).get("userId")).longValue();
            String ip = request.getRemoteAddr();
            String details = String.format("Executed %s with args: %s",
                    joinPoint.getSignature().getName(), joinPoint.getArgs());

            auditLogRepository.save(new AuditLog(userId, auditable.action(), details, ip));
        } catch (Exception ignored) {}
    }
}
```

```java
package com.moneytransfer.audit;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    String action();
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/moneytransfer/audit/AuditAspect.java
git commit -m "feat: add audit logging AOP aspect"
```

---

### Task 17: Dashboard + Export + WebSocket

**Files:**
- Create: `src/main/java/com/moneytransfer/dashboard/DashboardService.java`
- Create: `src/main/java/com/moneytransfer/dashboard/DashboardController.java`
- Create: `src/main/java/com/moneytransfer/export/ExportService.java`
- Create: `src/main/java/com/moneytransfer/export/ExportController.java`
- Create: `src/main/java/com/moneytransfer/config/WebSocketConfig.java`
- Create: `src/main/java/com/moneytransfer/notification/NotificationWebSocketHandler.java`

To keep plan length manageable, these follow the same pattern as prior tasks. Key code signatures:

- DashboardService: `getStats(Long userId)` → returns total balance, recent transactions, 7d/30d chart data
- ExportService: `exportPdf(Long accountId, LocalDate from, LocalDate to)` using OpenPDF; `exportExcel(...)` using Apache POI
- WebSocketConfig: registers STOMP endpoint `/ws/notifications` with SockJS
- NotificationWebSocketHandler: sends TRANSFER_RECEIVED and RECURRING_PAYMENT_EXECUTED events

- [ ] **Commit (after all three)**

```bash
git add src/main/java/com/moneytransfer/dashboard/ src/main/java/com/moneytransfer/export/ src/main/java/com/moneytransfer/config/WebSocketConfig.java src/main/java/com/moneytransfer/notification/
git commit -m "feat: implement dashboard, export, and WebSocket notifications"
```

---

### Task 18: Admin module

**Files:**
- Create: `src/main/java/com/moneytransfer/admin/AdminService.java`
- Create: `src/main/java/com/moneytransfer/admin/AdminController.java`

- [ ] **Step 1: Create AdminService.java**

```java
package com.moneytransfer.admin;

import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.audit.AuditLogRepository;
import com.moneytransfer.user.User;
import com.moneytransfer.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    public Page<User> getUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    @Transactional
    public void lockUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setLocked(true);
        userRepository.save(user);
    }

    @Transactional
    public void unlockUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        user.setLocked(false);
        user.setFailedAttempts(0);
        userRepository.save(user);
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }
}
```

- [ ] **Step 2: Create AdminController.java**

```java
package com.moneytransfer.admin;

import com.moneytransfer.audit.AuditLog;
import com.moneytransfer.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public Page<User> getUsers(@RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        return adminService.getUsers(PageRequest.of(page, size));
    }

    @PostMapping("/lock-user/{id}")
    public ResponseEntity<?> lockUser(@PathVariable Long id) {
        adminService.lockUser(id);
        return ResponseEntity.ok(Map.of("message", "User locked"));
    }

    @PostMapping("/unlock-user/{id}")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        adminService.unlockUser(id);
        return ResponseEntity.ok(Map.of("message", "User unlocked"));
    }

    @GetMapping("/audit-logs")
    public Page<AuditLog> getAuditLogs(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return adminService.getAuditLogs(PageRequest.of(page, size));
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/moneytransfer/admin/
git commit -m "feat: implement admin module"
```

---

### Task 19: Thymeleaf page controllers

**Files:**
- Modify: `src/main/java/com/moneytransfer/auth/AuthController.java` (add GET /login, /register)
- Modify: `src/main/java/com/moneytransfer/dashboard/DashboardController.java` (add GET /dashboard)
- Create: `src/main/java/com/moneytransfer/WebController.java` (for remaining page routes)

- [ ] **Step 1: Add login/register page routes to AuthController**

```java
@GetMapping("/login")
public String loginPage() { return "login"; }

@GetMapping("/register")
public String registerPage() { return "register"; }
```

- [ ] **Step 2: Add dashboard page route to DashboardController**

```java
@GetMapping("/dashboard")
public String dashboardPage(Model model, Authentication auth) {
    Claims claims = (Claims) auth.getDetails();
    Long userId = ((Integer) claims.get("userId")).longValue();
    model.addAttribute("accounts", accountService.getUserAccounts(userId));
    return "dashboard";
}
```

- [ ] **Step 3: Create WebController.java**

```java
package com.moneytransfer;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/transfer") public String transfer() { return "transfer"; }
    @GetMapping("/history") public String history() { return "history"; }
    @GetMapping("/beneficiaries") public String beneficiaries() { return "beneficiaries"; }
    @GetMapping("/scheduled") public String scheduled() { return "scheduled"; }
    @GetMapping("/profile") public String profile() { return "profile"; }
    @GetMapping("/admin/users") public String adminUsers() { return "admin/users"; }
    @GetMapping("/admin/transactions") public String adminTransactions() { return "admin/transactions"; }
}
```

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/moneytransfer/WebController.java
git commit -m "feat: add page controllers for Thymeleaf routes"
```

---

### Task 20: Full application build + integration test

**Files:** All existing files

- [ ] **Step 1: Full project compilation**

Run: `mvn clean compile -q`
Expected: BUILD SUCCESS

- [ ] **Step 2: Run all unit tests**

Run: `mvn test -q`
Expected: All tests pass

- [ ] **Step 3: Create application integration test**

```java
package com.moneytransfer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MoneyTransferApplicationTests {
    @Test
    void contextLoads() {}
}
```

- [ ] **Step 4: Build executable JAR**

Run: `mvn package -DskipTests -q`
Expected: BUILD SUCCESS, JAR created in `target/money-transfer-1.0.0.jar`

- [ ] **Step 5: Commit**

```bash
git add src/test/java/com/moneytransfer/MoneyTransferApplicationTests.java
git commit -m "chore: add integration test and finalize build"
```


