CREATE DATABASE IF NOT EXISTS money_transfer CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE money_transfer;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role ENUM('USER','ADMIN') NOT NULL,
    tier ENUM('STANDARD','VIP') NOT NULL,
    avatar_url VARCHAR(500),
    face_image_url VARCHAR(500),
    face_encoding TEXT,
    face_enabled BIT(1) NOT NULL DEFAULT 0,
    pin_hash VARCHAR(255),
    pin_set TINYINT(1) NOT NULL DEFAULT 0,
    otp_enabled BIT(1) NOT NULL DEFAULT 0,
    email_notifications BIT(1) NOT NULL DEFAULT 0,
    language VARCHAR(5),
    failed_attempts INT NOT NULL DEFAULT 0,
    locked BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_number VARCHAR(20) NOT NULL UNIQUE,
    balance DECIMAL(15,2) NOT NULL,
    balance_checksum VARCHAR(64),
    status ENUM('ACTIVE','FROZEN','CLOSED') NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE transactions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    transaction_code VARCHAR(20) NOT NULL UNIQUE,
    from_account_id BIGINT,
    to_account_id BIGINT,
    amount DECIMAL(15,2) NOT NULL,
    from_balance_after DECIMAL(15,2),
    to_balance_after DECIMAL(15,2),
    type ENUM('TRANSFER','WITHDRAW','DEPOSIT','RECURRING') NOT NULL,
    status ENUM('PENDING','COMPLETED','FAILED') NOT NULL,
    description VARCHAR(255),
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE beneficiaries (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    account_number VARCHAR(20) NOT NULL,
    nickname VARCHAR(100),
    bank_name VARCHAR(100),
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE recent_transfers (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    to_account_id BIGINT NOT NULL,
    to_account_number VARCHAR(20) NOT NULL,
    to_account_name VARCHAR(255),
    transferred_at DATETIME NOT NULL
);

CREATE TABLE scheduled_payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    from_account_id BIGINT NOT NULL,
    to_account_number VARCHAR(20) NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    frequency ENUM('WEEKLY','MONTHLY') NOT NULL,
    status ENUM('ACTIVE','PAUSED','CANCELLED') NOT NULL,
    description VARCHAR(255),
    start_date DATETIME(6) NOT NULL,
    end_date DATETIME(6),
    next_run DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL
);

CREATE TABLE otp_codes (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL,
    type ENUM('LOGIN','TRANSACTION') NOT NULL,
    used BIT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    expires_at DATETIME(6) NOT NULL
);

CREATE TABLE audit_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    details TEXT,
    ip_address VARCHAR(45),
    created_at DATETIME(6) NOT NULL
);
