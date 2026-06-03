# Fashion Advisor — Hệ Thống Nhận Diện Trang Phục & Gợi Ý Phối Đồ Thông Minh

## 1. Tổng quan

Hệ thống web app cho phép người dùng upload ảnh quần áo, AI tự động nhận diện loại/màu sắc/hoạ tiết, lưu vào tủ đồ cá nhân, và gợi ý phối đồ thông minh dựa trên màu sắc, loại đồ, dịp, thời tiết.

## 2. Stack công nghệ

- **Backend:** Spring Boot 3.2, Java 21
- **Frontend:** Thymeleaf, CSS (banking-style), JavaScript
- **Database:** MySQL (script SQL riêng, `ddl-auto: none`)
- **AI:** DJL (Deep Java Library) + ResNet50 — image classification
- **Auth:** JWT (tái sử dụng từ dự án money-transfer)
- **Ảnh:** Lưu base64 trong MySQL (kiểu `LONGTEXT`, đủ chứa ảnh lên đến ~4GB)

## 3. Kiến trúc

```
[Thymeleaf UI] → [Spring Boot Controller] → [Service Layer]
                                                ├── ClothingRecognitionService (DJL)
                                                ├── ColorAnalysisService (BufferedImage)
                                                └── OutfitSuggestionService (rule-based)
                                                      ↓
                                                MySQL
                                              ├── users
                                              ├── clothing_items
                                              ├── outfits
                                              └── outfit_items
```

## 4. Database

File SQL riêng (`schema.sql`) chạy bằng MySQL Workbench.

**Tables (kèm Foreign Key):**

- `users(id)` — bảng cha, liên kết với `clothing_items.user_id` và `outfits.user_id`
- `clothing_items(id)` — `user_id` → `users(id)`, kiểu `image_base64 LONGTEXT NOT NULL` (đủ chứa ảnh gốc encode base64 lên đến ~4GB)
- `outfits(id)` — `user_id` → `users(id)`
- `outfit_items(id)` — `outfit_id` → `outfits(id)`, `item_id` → `clothing_items(id)`

## 5. Luồng nhận diện ảnh (DJL)

1. User upload ảnh → gửi base64 lên server
2. DJL ResNet50 phân loại → trả label (shirt, jeans, sneaker, dress...)
3. **ColorAnalysisService** — phân tích màu sắc chủ đạo bằng thuật toán **K-Means Clustering**:
   - Gom $k = 5$ cluster từ các pixel ảnh (không gian màu RGB)
   - Chọn cluster có kích thước lớn nhất làm màu chủ đạo
   - Chuyển đổi RGB → HSV để lấy tên màu tự nhiên (hue angle $H \in [0^\circ, 360^\circ)$)
   - Xuất: `color_name` (xanh dương, đỏ...) + `color_hex` (#0000ff)
4. Pattern detection (edge detection đơn giản)
5. Hiển thị kết quả → user xác nhận/chỉnh sửa
6. Lưu vào `clothing_items`

**Model:** ResNet50 từ DJL Model Zoo (ImageNet, ~50 clothing classes)

## 6. Gợi ý phối đồ (Rule-based)

**a. Color Harmony (dùng tọa độ HSV Color Wheel):**
- Chuyển màu sắc từ RGB → HSV, lấy góc $H$ trên bánh xe màu ( $0^\circ \leq H < 360^\circ$ )
- **Complementary:** $\Delta H = 180^\circ \pm 15^\circ$ — màu đối diện, tương phản mạnh
- **Analogous:** $\Delta H \leq 30^\circ$ — màu kề cạnh, hài hoà nhẹ nhàng
- **Monochrome:** cùng $H$, khác $S$ (bão hoà) và $V$ (độ sáng) — cùng tông màu

**b. Type Compatibility:**
- Top + Bottom + Shoes (cơ bản)
- Dress + Shoes + Accessory
- Jacket layer ngoài bất kỳ outfit

**c. Occasion + Season Filter:**
- Formal → shirt + trousers + leather shoes
- Casual → t-shirt + jeans + sneakers
- Party → dress + heels
- Summer → sáng màu, chất liệu mỏng
- Winter → jacket, tối màu

## 7. API Endpoints

| Method | Path | Chức năng |
|--------|------|-----------|
| POST | /api/auth/register | Đăng ký |
| POST | /api/auth/login | Đăng nhập |
| POST | /api/clothing/recognize | Upload + AI nhận diện |
| POST | /api/clothing/items | Thêm item (sau khi xác nhận) |
| GET | /api/clothing/items | Danh sách tủ đồ |
| DELETE | /api/clothing/items/{id} | Xoá item |
| PUT | /api/clothing/items/{id} | Sửa item |
| POST | /api/outfits/suggest | Gợi ý phối đồ |
| POST | /api/outfits | Lưu outfit |
| GET | /api/outfits | Danh sách outfit |
| GET | /api/outfits/{id} | Chi tiết outfit |

## 8. Trang Web

| URL | Chức năng |
|-----|-----------|
| /login | Đăng nhập |
| /register | Đăng ký |
| /dashboard | Tổng quan tủ đồ |
| /wardrobe | Quản lý tủ đồ |
| /upload | Upload + nhận diện ảnh |
| /outfits | Gợi ý + quản lý outfit |

## 9. Thư mục dự án

```
fashion-advisor/
├── pom.xml
├── schema.sql
├── src/main/java/com/fashionadvisor/
│   ├── FashionAdvisorApplication.java
│   ├── config/         (Security, JWT, RestTemplate)
│   ├── auth/           (JwtUtil, JwtAuthFilter, AuthController, AuthService)
│   ├── user/           (User, UserRepository)
│   ├── clothing/       (ClothingItem, ClothingController, ClothingService)
│   ├── outfit/         (Outfit, OutfitController, OutfitSuggestionService)
│   ├── recognition/    (DJL Recognition + ColorAnalysis)
│   └── web/            (DashboardController, WardrobeController...)
├── src/main/resources/
│   ├── application.yml
│   ├── static/css/
│   ├── static/js/
│   └── templates/
└── docs/
```
