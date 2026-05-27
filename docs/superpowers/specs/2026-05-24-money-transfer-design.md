# Money Transfer Web Application - Design Document

## 1. Overview

A web-based money transfer system built by a team of 2-3 intermediate Java developers over 2 weeks. The system supports user authentication (JWT + face login + OTP), money transfers, recurring payments, QR code payments, and an admin dashboard.

## 2. Architecture

**Monolith: Spring Boot + Thymeleaf (port 8080)**

- Single Spring Boot application serving both REST API and Thymeleaf views
- Frontend: Thymeleaf + Alpine.js for dynamic components (dashboard charts, notifications)
- Database: MySQL
- Auth: JWT (access + refresh token) with Redis blacklist on logout
- Build: Maven

## 3. Database Schema

### users
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| username | VARCHAR(50) UNIQUE NOT NULL | |
| password_hash | VARCHAR(255) NOT NULL | BCrypt |
| email | VARCHAR(100) UNIQUE NOT NULL | |
| phone | VARCHAR(20) | |
| full_name | VARCHAR(100) NOT NULL | |
| face_encoding | TEXT | OpenCV vector as comma-separated doubles |
| face_image_url | VARCHAR(500) | S3/MinIO URL, not raw image |
| tier | ENUM('STANDARD','VIP') DEFAULT 'STANDARD' | |
| otp_enabled | BOOLEAN DEFAULT TRUE | toggle in profile |
| face_enabled | BOOLEAN DEFAULT FALSE | toggle in profile, requires face registered |
| locked | BOOLEAN DEFAULT FALSE | Auto-lock after 5 failed attempts |
| failed_attempts | INT DEFAULT 0 | |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### accounts
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| user_id | BIGINT FK -> users.id | |
| account_number | VARCHAR(20) UNIQUE NOT NULL | |
| balance | DECIMAL(15,2) DEFAULT 0 | |
| balance_checksum | VARCHAR(64) | SHA-256(account_number + balance + secret_key) |
| status | ENUM('ACTIVE','FROZEN','CLOSED') DEFAULT 'ACTIVE' | |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### transactions
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| transaction_code | VARCHAR(20) UNIQUE NOT NULL | e.g. TX20260524XXXXXX |
| from_account_id | BIGINT FK -> accounts.id | NULL for deposits |
| to_account_id | BIGINT FK -> accounts.id | NULL for withdrawals |
| amount | DECIMAL(15,2) NOT NULL | |
| from_balance_after | DECIMAL(15,2) | sender balance after tx |
| to_balance_after | DECIMAL(15,2) | receiver balance after tx |
| type | ENUM('TRANSFER','WITHDRAW','DEPOSIT','RECURRING') | |
| status | ENUM('PENDING','COMPLETED','FAILED') DEFAULT 'COMPLETED' | |
| description | VARCHAR(255) | |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### beneficiaries
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| user_id | BIGINT FK -> users.id | |
| account_number | VARCHAR(20) NOT NULL | |
| nickname | VARCHAR(100) | |
| bank_name | VARCHAR(100) DEFAULT 'INTERNAL' | |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### scheduled_payments
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| from_account_id | BIGINT FK -> accounts.id | |
| to_account_number | VARCHAR(20) NOT NULL | |
| amount | DECIMAL(15,2) NOT NULL | |
| description | VARCHAR(255) | |
| frequency | ENUM('WEEKLY','MONTHLY') | |
| next_run | DATE NOT NULL | |
| status | ENUM('ACTIVE','PAUSED','CANCELLED') DEFAULT 'ACTIVE' | |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### audit_logs
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| user_id | BIGINT FK -> users.id | |
| action | VARCHAR(100) NOT NULL | e.g. LOGIN, TRANSFER, EXPORT |
| details | TEXT | JSON payload |
| ip_address | VARCHAR(45) | |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### otp_codes
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| user_id | BIGINT FK -> users.id | |
| code | VARCHAR(6) NOT NULL | |
| type | ENUM('LOGIN','TRANSACTION') | |
| expires_at | TIMESTAMP NOT NULL | 5-minute TTL |
| used | BOOLEAN DEFAULT FALSE | |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

### admin_actions
| Column | Type | Notes |
|--------|------|-------|
| id | BIGINT PK AUTO_INCREMENT | |
| admin_id | BIGINT FK -> users.id | |
| target_user_id | BIGINT FK -> users.id | |
| action | VARCHAR(100) NOT NULL | LOCK_USER, UNLOCK_USER, APPROVE_TX |
| created_at | TIMESTAMP DEFAULT CURRENT_TIMESTAMP | |

## 4. Authentication Flow

### Login
1. User enters username/password
2. If OTP enabled: send OTP via email, verify
3. If face enabled: optional face verification step
4. Issue JWT (access 15min + refresh 7d), store in httpOnly cookie
5. Record audit log

### Logout
1. Add access token to Redis blacklist (TTL = remaining token life)
2. Clear cookies

### Security
- Spring Security filter chain: JwtAuthFilter (extract token from cookie, check blacklist)
- CSRF disabled for API, enabled for form-based auth
- Rate limiting on OTP endpoints (1 per 60s per user)
- Account lockout after 5 failed login attempts (reset after 15 min)

## 5. Concurrency (Pessimistic Locking)

### Deadlock Prevention

Lock accounts in a fixed order (by ID ascending) to prevent circular wait deadlocks. Example: if User A (ID=1) sends to User B (ID=2) and simultaneously User B sends to User A, both threads will lock Account 1 first, then Account 2 — one will wait, the other will proceed, then release.

Transaction pattern:

```
@Transactional
public void transfer(Long fromAccountId, Long toAccountId, BigDecimal amount) {
    // Sort IDs to prevent deadlock: always lock smaller ID first
    Long firstId = Math.min(fromAccountId, toAccountId);
    Long secondId = Math.max(fromAccountId, toAccountId);

    Account first = accountRepo.findByIdWithLock(firstId);   // @Lock(PESSIMISTIC_WRITE)
    Account second = accountRepo.findByIdWithLock(secondId);

    // Assign from/to after locking (swap if needed)
    Account from = first.getId().equals(fromAccountId) ? first : second;
    Account to   = first.getId().equals(fromAccountId) ? second : first;

    validateBalance(from, amount);
    from.setBalance(from.getBalance().subtract(amount));
    to.setBalance(to.getBalance().add(amount));

    // Update checksums
    from.setBalanceChecksum(computeChecksum(from));
    to.setBalanceChecksum(computeChecksum(to));

    accountRepo.save(from);
    accountRepo.save(to);

    String txCode = generateTransactionCode();
    transactionRepo.save(new Transaction(txCode, from.getId(), to.getId(),
        amount, from.getBalance(), to.getBalance(), ...));
}
```

`AccountRepository`:
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT a FROM Account a WHERE a.id = :id")
Account findByIdWithLock(@Param("id") Long id);
```

### Balance Integrity (Checksum)

On every `Account` load, compute SHA-256(`account_number` + `balance` + `secret_key`) and compare against `balance_checksum`. Mismatch → set status = FROZEN, log audit, notify admin immediately. An `EntityListener` (or repository interceptor) on `@PostLoad` and `@PreUpdate` handles this transparently.

### After-Transaction Balances

Each transaction stores `from_balance_after` and `to_balance_after` (snapshot at execution time). This enables reconciliation without replaying history.

## 6. Face Login

- Registration: user uploads photo → Spring Boot crops face via OpenCV Haar Cascade → computes face encoding → uploads original to S3/MinIO → stores URL + encoding vector in DB
- Login: user uploads photo → extract encoding → compare with stored encoding using cosine similarity (threshold 0.7)

### CPU/RAM Protection

OpenCV via JNI is CPU-intensive. Two safeguards:

1. **Image size limit**: All uploaded face images are resized to max 400x400 pixels before OpenCV processing (reduces matrix computation cost).
2. **Dedicated ThreadPoolExecutor**: Face processing runs on a separate thread pool (2-4 threads) isolated from Tomcat's request threads. If face login queues up, transfer/withdraw requests remain unaffected.

```java
@Configuration
public class FaceProcessingConfig {
    @Bean("faceExecutor")
    public Executor faceExecutor() {
        return Executors.newFixedThreadPool(4);  // max 4 concurrent face ops
    }
}
```

## 7. OTP via Email

- Uses JavaMailSender (Spring Boot mail starter)
- 6-digit random code, 5-minute expiry
- Sent on: login (if OTP enabled), high-value transactions (above user's tier limit)

## 8. Modules & API Endpoints

### Auth
- POST /api/auth/register
- POST /api/auth/login
- POST /api/auth/logout
- POST /api/auth/refresh-token

### OTP
- POST /api/otp/send (email)
- POST /api/otp/verify

### Face
- POST /api/face/register (upload + encode)
- POST /api/face/login (verify)

### User
- GET /api/users/profile
- PUT /api/users/profile
- GET /api/users/beneficiaries
- POST /api/users/beneficiaries
- DELETE /api/users/beneficiaries/{id}

### Account
- GET /api/accounts
- GET /api/accounts/{id}

### Transaction
- POST /api/transactions/transfer
- POST /api/transactions/withdraw
- POST /api/transactions/deposit
- GET /api/transactions/history (paginated, with date filter)
- GET /api/transactions/{id}

### QR
- GET /api/qr/generate?account={id}
- POST /api/qr/scan

### Scheduled Payments
- POST /api/scheduled-payments
- GET /api/scheduled-payments
- DELETE /api/scheduled-payments/{id}

### Dashboard
- GET /api/dashboard/stats

### Export
- GET /api/export/statement?from=&to= (PDF with iText, Excel with Apache POI)

### Admin
- GET /api/admin/users
- POST /api/admin/lock-user/{id}
- POST /api/admin/unlock-user/{id}
- GET /api/admin/transactions/pending
- POST /api/admin/approve-transaction/{id}
- GET /api/admin/audit-logs

## 9. WebSocket Notifications

- Endpoint: /ws/notifications (STOMP over SockJS)
- Events: TRANSFER_RECEIVED, RECURRING_PAYMENT_EXECUTED, ACCOUNT_LOCKED, OTP_SENT

## 10. Frontend Pages (Thymeleaf + Alpine.js)

- `/` — Login page (with face login toggle)
- `/register` — Registration form
- `/dashboard` — Overview with balance, recent transactions, charts (7d/30d)
- `/transfer` — Money transfer form (supports QR scan + beneficiary select)
- `/history` — Transaction history with date range filter + export buttons
- `/beneficiaries` — Manage saved beneficiaries
- `/scheduled` — View/manage recurring payments
- `/profile` — Edit profile, upload face, OTP settings
- `/admin/*` — Admin panel (users list, lock/unlock, pending transactions)

## 11. Technology Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 17, Spring Boot 3.x, Spring Security, Spring Data JPA |
| Frontend | Thymeleaf, Alpine.js, Chart.js, Tailwind CSS |
| Database | MySQL 8.x |
| Cache/Blacklist | Redis |
| Face Detection | OpenCV (opencv-java) |
| OTP | JavaMailSender |
| QR | ZXing (com.google.zxing) |
| PDF | iText / OpenPDF |
| Excel | Apache POI |
| Cloud Storage | MinIO (local dev) or AWS S3 |
| Build | Maven |

## 12. Project Structure

```
money-transfer/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/moneytransfer/
│   │   │   ├── MoneyTransferApplication.java
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebSocketConfig.java
│   │   │   │   ├── RedisConfig.java
│   │   │   │   └── StorageConfig.java (S3/MinIO)
│   │   │   ├── auth/
│   │   │   │   ├── AuthController.java
│   │   │   │   ├── AuthService.java
│   │   │   │   ├── JwtUtil.java
│   │   │   │   ├── JwtAuthFilter.java
│   │   │   │   └── TokenBlacklistService.java (Redis)
│   │   │   ├── user/
│   │   │   │   ├── UserController.java
│   │   │   │   ├── UserService.java
│   │   │   │   ├── User.java (entity)
│   │   │   │   └── UserRepository.java
│   │   │   ├── account/
│   │   │   │   ├── AccountController.java
│   │   │   │   ├── AccountService.java
│   │   │   │   ├── Account.java (entity)
│   │   │   │   └── AccountRepository.java
│   │   │   ├── transaction/
│   │   │   │   ├── TransactionController.java
│   │   │   │   ├── TransactionService.java
│   │   │   │   ├── Transaction.java (entity)
│   │   │   │   └── TransactionRepository.java
│   │   │   ├── otp/
│   │   │   │   ├── OtpController.java
│   │   │   │   ├── OtpService.java
│   │   │   │   ├── OtpCode.java (entity)
│   │   │   │   └── OtpRepository.java
│   │   │   ├── face/
│   │   │   │   ├── FaceController.java
│   │   │   │   ├── FaceService.java
│   │   │   │   └── FaceRecognitionUtil.java (OpenCV)
│   │   │   ├── qr/
│   │   │   │   ├── QrController.java
│   │   │   │   └── QrService.java
│   │   │   ├── scheduled/
│   │   │   │   ├── ScheduledPaymentController.java
│   │   │   │   ├── ScheduledPaymentService.java
│   │   │   │   ├── ScheduledPayment.java (entity)
│   │   │   │   ├── ScheduledPaymentRepository.java
│   │   │   │   └── RecurringPaymentJob.java (@Scheduled)
│   │   │   ├── beneficiary/
│   │   │   │   ├── BeneficiaryController.java
│   │   │   │   ├── BeneficiaryService.java
│   │   │   │   ├── Beneficiary.java (entity)
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
│   │   │   │   ├── AuditLog.java (entity)
│   │   │   │   ├── AuditLogRepository.java
│   │   │   │   ├── AuditAspect.java (AOP)
│   │   │   │   └── AdminAction.java (entity)
│   │   │   └── notification/
│   │   │       └── NotificationWebSocketHandler.java
│   │   ├── resources/
│   │   │   ├── application.yml
│   │   │   ├── templates/
│   │   │   │   ├── layout.html (Thymeleaf layout)
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
├── docker-compose.yml (MySQL + Redis + MinIO)
└── docs/
    └── superpowers/
        └── specs/
            └── 2026-05-24-money-transfer-design.md
```
