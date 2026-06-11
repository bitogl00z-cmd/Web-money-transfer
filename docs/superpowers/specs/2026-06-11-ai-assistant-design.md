# AI Assistant Design — Money Transfer Web

**Ngày:** 2026-06-11
**Mục tiêu:** Tích hợp AI assistant hỗ trợ thanh toán và quản lý người dùng bằng ngôn ngữ tự nhiên (tiếng Việt) cho web chuyển tiền Spring Boot.

---

## 1. Kiến trúc tổng thể

```
Google Colab                              Máy local / Server
─────────────────                        ───────────────────
Train data (500 câu)                      Python FastAPI :5000
       ↓                                         │
 Fine-tune / Train model                         │ POST /api/ai/parse
       ↓                                         │
 Export model (.pkl / .pt) ───── copy ───→   load model
                                              (init 1 lần)

Java Spring Boot :8080
  ┌─ Web UI ─┐
  │ Chat box  │ ──→ /api/ai/chat (Java)
  └───────────┘         │
                        ↓ POST localhost:5000/api/ai/parse
                        ↓
                 { intent: "transfer",
                   entities: { amount: 500000, target: "user2" } }
                        ↓
                 Java thực thi chuyển tiền / tra cứu / quản lý
```

**Nguyên tắc:**
- Python chịu trách nhiệm hiểu ngôn ngữ tự nhiên (NLP)
- Java chịu trách nhiệm business logic (chuyển tiền, CRUD user, tra cứu DB)
- Giao tiếp qua REST JSON

---

## 2. Dữ liệu train

### Intents

| Intent | Mô tả | Số câu mẫu | Ví dụ |
|--------|-------|-------------|-------|
| `transfer` | Chuyển tiền | 120 | "chuyển 500k cho user2" |
| `balance` | Xem số dư | 80 | "tài khoản còn bao nhiêu" |
| `history` | Lịch sử giao dịch | 80 | "xem giao dịch 7 ngày qua" |
| `create_user` | Tạo người dùng | 60 | "tạo user mới tên Hùng" |
| `help` | Trợ giúp | 40 | "ai có thể làm gì" |
| `greeting` | Chào hỏi | 40 | "xin chào", "hello" |
| `fallback` | Không hiểu | 80 | Câu ngoài luồng |

**Tổng cộng:** ~500 câu

### Entities
- `amount` — số tiền (500k, 1 triệu, 2000000)
- `target` — người nhận (username, tên)
- `date_range` — khoảng thời gian (7 ngày qua, tháng này)

---

## 3. Model

**Chọn PhoBERT (khuyên dùng)**

- Model: `vinai/phobert-base` (pre-trained BERT tiếng Việt)
- Task: Sequence classification (7 intents) + entity extraction regex bổ trợ
- Framework: PyTorch + Transformers + `phobert-base` tokenizer
- Pipeline:
  - Input: câu tiếng Việt
  - Tokenize → PhoBERT → Linear head → softmax 7 lớp
  - Output: intent + confidence score
- Entity extraction: dùng regex + rule đơn giản sau khi biết intent (không cần NER model riêng)

**Train trên Google Colab:**
- GPU T4 free
- ~5 phút với 500 câu
- Export file `.pt` hoặc `.pkl`
- Dung lượng: ~700MB

**Inference trên CPU:**
- Load model 1 lần khi khởi động
- Thời gian response < 1s / câu

---

## 4. Python FastAPI server

### File structure
```
python-ai/
├── main.py              # FastAPI server
├── model.py             # Load model + predict
├── requirements.txt     # Dependencies
└── models/
    └── phobert-intent.pt # Trained model
```

### API contract

**`POST /api/ai/parse`**

Request:
```json
{ "text": "chuyển 500k cho user2" }
```

Response:
```json
{
  "intent": "transfer",
  "confidence": 0.92,
  "entities": {
    "amount": 500000,
    "target": "user2"
  }
}
```

### Error response:
```json
{
  "intent": "fallback",
  "confidence": 0.0,
  "entities": {}
}
```

### Khởi động
```bash
cd python-ai
pip install -r requirements.txt
python main.py   # chạy trên port 5000
```

---

## 5. Java Spring Boot integration

### Service mới: `AiService.java`

```
JavaController                              AiService
─────────────                              ──────────
/api/ai/chat ──→  POST localhost:5000       parse(text)
  (nhận text     /api/ai/parse              → intent + entities
   từ chat UI)         │
                       ↓
                  switch(intent):
                    "transfer"   → TransactionService.transfer()
                    "balance"    → AccountService.getBalance()
                    "history"    → TransactionService.getRecent()
                    "create_user" → UserService.createUser()
                    "help"       → response template
                    "greeting"   → response template
                    "fallback"   → "Xin lỗi, tôi chưa hiểu..."
```

### Chat Web UI

Thêm chat widget trong layout hoặc trang riêng `/ai`:
- Input text + nút gửi
- Hiển thị hội thoại (user + bot)
- Gọi `/api/ai/chat` (Java), Java gọi Python → thực thi → trả kết quả

---

## 6. Luồng xử lý mẫu

**User:** "chuyển 500k cho user2"
```
1. Chat UI → POST /api/ai/chat { text: "chuyển 500k cho user2" }
2. Java AiService → POST localhost:5000/api/ai/parse
3. Python → { intent: "transfer", entities: { amount: 500000, target: "user2" } }
4. Java → TransactionService.transfer(currentUser, "user2", 500000, "Chuyển qua AI")
5. Java → Response { message: "Đã chuyển 500.000₫ cho user2 thành công" }
6. Chat UI hiển thị kết quả
```

---

## 7. File thay đổi (Java)

| File | Thay đổi |
|------|----------|
| `src/main/java/com/moneytransfer/ai/AiController.java` | MỚI — `POST /api/ai/chat` |
| `src/main/java/com/moneytransfer/ai/AiService.java` | MỚI — gọi Python, thực thi intent |
| `src/main/java/com/moneytransfer/config/AiConfig.java` | MỚI — cấu hình URL Python server |
| `src/main/resources/templates/ai.html` | MỚI — chat UI |
| `src/main/resources/static/css/banking.css` | Thêm style chat widget |

---

## 8. Kế hoạch thực hiện

1. Xây dựng dataset 500 câu tiếng Việt gán nhãn
2. Train PhoBERT trên Colab, export model
3. Viết Python FastAPI server + load model
4. Viết Java AiController + AiService
5. Tạo chat Web UI
6. Kết nối end-to-end, kiểm thử
