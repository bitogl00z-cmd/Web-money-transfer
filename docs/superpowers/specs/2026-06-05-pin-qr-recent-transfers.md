# PIN, QR Payment & Recent Transfers Design

## Overview
Add three features to the existing money-transfer web app: PIN-based 2FA, QR code payment (demo), and recent transfer accounts quick-select.

## 1. Mã PIN (2FA)

### Backend
- **User.java**: Add `pinHash` (String, nullable), `pinSet` (Boolean, default false)
- **PinController** (`/api/pin/**`):
  - `POST /api/pin/set` — Set PIN (requires auth, body: `{pin: "123456"}`)
  - `POST /api/pin/verify` — Verify PIN (body: `{pin: "123456"}`, returns `{valid: true}` or 400)
  - `POST /api/pin/remove` — Remove PIN (verify old PIN first)
- **PinService**: Hash PIN with BCrypt, verify against stored hash
- **Luồng login**: `AuthController.login()` returns `pinRequired: true` nếu user có PIN → FE redirect sang `/pin-verify`
- **Luồng transfer**: `TransactionController.transfer()` kiểm tra `x-pin` header → gọi `pinService.verify()` trước khi xử lý giao dịch

### Frontend
- **Trang `/pin-verify`**: Form nhập PIN 4-6 số, gọi `/api/pin/verify` → set cookie `pin_verified` (JWT, 5 phút) → redirect dashboard
- **Trang `/settings`**: Form cài PIN (nhập + xác nhận), nút "Xóa PIN" nếu đã có
- **Modal trên Transfer**: Gọi `apiFetch('/api/pin/verify', ...)` trước khi submit transfer. Nếu valid → gửi transfer kèm header `x-pin`

## 2. QR Payment (Demo)

### Library
- `qrcodejs` — sinh QR code trên client
- `jsQR` — quét QR code từ camera

### Hiển thị QR
- **Trang `/settings`** — card "Mã QR của tôi" sinh QR từ accountNumber
- **Trang `/dashboard`** — nút "QR Code" mở modal show QR

### Quét QR
- **Trang `/transfer`** — nút "📷 Quét QR" trong input "Đến tài khoản"
- Mở camera → chụp → jsQR decode → auto-fill số tài khoản đích
- **Demo**: QR chỉ chứa `acct:MTXXXXXXXXXXXXXX` (định dạng đơn giản), không có checksum thật

## 3. Tài khoản gần đây (Recent Transfers)

### Backend
- **Entity RecentTransfer**: `id, userId, toAccountId, toAccountNumber, toAccountName, transferredAt`
- **RecentTransferRepository**: `findTop5ByUserIdOrderByTransferredAtDesc(userId)`
- **RecentTransferService**: Ghi record sau mỗi `transfer()` thành công
- **TransactionController.transfer()** → sau khi thành công, ghi RecentTransfer
- **GET /api/recent-transfers** — trả về 5 gần nhất

### Frontend
- **Trang `/transfer`**: Load `/api/recent-transfers`, hiển thị danh sách dạng các nút/card nhỏ
- Click vào → điền số tài khoản đích + tên người nhận

## Files affected

### New files:
- `src/main/java/com/moneytransfer/pin/PinController.java`
- `src/main/java/com/moneytransfer/pin/PinService.java`
- `src/main/java/com/moneytransfer/recent/RecentTransfer.java`
- `src/main/java/com/moneytransfer/recent/RecentTransferRepository.java`
- `src/main/java/com/moneytransfer/recent/RecentTransferService.java`
- `src/main/resources/templates/pin-verify.html`

### Modified files:
- `user/User.java` — add pinHash, pinSet
- `user/UserRepository.java` — (if needed)
- `auth/AuthController.java` — return pinRequired in login response
- `auth/AuthService.java` — set pinSet on register
- `transaction/TransactionController.java` — add PIN check, record RecentTransfer
- `transaction/TransactionService.java` — (no change needed)
- `WebController.java` — add `/pin-verify` mapping
- `SecurityConfig.java` — permit `/api/pin/**` (verify endpoint), add `/pin-verify` to authenticated (already .anyRequest().authenticated())
- `settings.html` — add PIN form + QR card
- `dashboard.html` — add QR button
- `transfer.html` — add QR scan button, recent transfers list, PIN modal
- `banking.js` — add QR lib loading, QR generation/scanning helpers

## Implementation Order
1. User entity + PIN backend (PinService, PinController)
2. Login PIN flow (pin-verify page, AuthController change)
3. Transfer PIN check
4. RecentTransfer entity + recording
5. Recent transfers UI
6. QR payment (settings QR, transfer QR scan)
