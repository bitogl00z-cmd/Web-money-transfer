# AI Assistant Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an AI assistant that understands Vietnamese natural language to support payments, balance lookup, history, and user management via a chat interface.

**Architecture:** Python FastAPI server runs a PhoBERT model fine-tuned on ~500 intent-labeled sentences. Java Spring Boot calls the Python server via REST to parse user input, then executes the corresponding business logic (transfer, balance, etc.). Communication is synchronous JSON over HTTP.

**Tech Stack:** Python 3.10+, FastAPI, PyTorch, Transformers, PhoBERT, Spring Boot 3.2.4, WebClient, Thymeleaf

---

### Task 1: Create dataset and Colab training notebook

**Files:**
- Create: `python-ai/data/intents.json`
- Create: `python-ai/train.ipynb`

- [ ] **Step 1: Write the intents dataset**

Create `python-ai/data/intents.json` with 500 labeled Vietnamese sentences:

```json
{
  "intents": [
    {
      "intent": "transfer",
      "patterns": [
        "chuyển 500k cho user2",
        "gửi cho Nam 1 triệu",
        "chuyển khoản 200000 cho user3",
        "chuyển tiền cho Hùng 500 nghìn",
        "gửi 300k đến tài khoản user4",
        "chuyển 1 triệu rưỡi cho bạn Hoa",
        "chuyển 2 triệu cho tài khoản 12345",
        "gửi 750000 cho user5",
        "chuyển 50k đến user6",
        "chuyển khoản 3 triệu cho user7",
        "gửi tiền 800k cho user8",
        "chuyển 1.2 triệu đến user9",
        "chuyển 5 trăm cho user10",
        "gửi 2 triệu rưỡi cho user11",
        "chuyển 4 triệu đến tài khoản 67890"
      ],
      "entities": ["amount", "target"]
    },
    {
      "intent": "balance",
      "patterns": [
        "xem số dư",
        "tài khoản còn bao nhiêu",
        "kiểm tra số dư",
        "số dư tài khoản",
        "xem tiền trong tài khoản",
        "còn bao nhiêu tiền",
        "cho tôi xem số dư",
        "số dư hiện tại",
        "kiểm tra tài khoản",
        "xem số dư tài khoản của tôi"
      ],
      "entities": []
    },
    {
      "intent": "history",
      "patterns": [
        "xem giao dịch gần đây",
        "lịch sử giao dịch",
        "giao dịch 7 ngày qua",
        "xem lịch sử chuyển tiền",
        "cho tôi xem giao dịch",
        "các giao dịch gần nhất",
        "lịch sử chuyển khoản",
        "xem các giao dịch đã thực hiện",
        "giao dịch tháng này",
        "sao kê gần đây"
      ],
      "entities": ["date_range"]
    },
    {
      "intent": "create_user",
      "patterns": [
        "tạo user mới tên Hùng",
        "tạo tài khoản cho Nam",
        "đăng ký người dùng mới tên Lan",
        "tạo user username hùng123",
        "thêm người dùng mới",
        "tạo tài khoản mới",
        "đăng ký thành viên mới",
        "tạo user cho nhân viên mới",
        "mở tài khoản cho Khánh",
        "tạo username mới"
      ],
      "entities": ["target"]
    },
    {
      "intent": "help",
      "patterns": [
        "ai có thể làm gì",
        "hỗ trợ",
        "bạn có thể làm gì",
        "trợ giúp",
        "hướng dẫn",
        "giúp tôi",
        "các tính năng",
        "bạn là ai",
        "chức năng của bạn",
        "có thể giúp gì"
      ],
      "entities": []
    },
    {
      "intent": "greeting",
      "patterns": [
        "xin chào",
        "hello",
        "chào bạn",
        "hi",
        "chào",
        "chào bot",
        "hey",
        "helo",
        "chào trợ lý",
        "xin chào trợ lý"
      ],
      "entities": []
    },
    {
      "intent": "fallback",
      "patterns": [
        "thời tiết hôm nay thế nào",
        "ai là tổng thống mỹ",
        "mấy giờ rồi",
        "bạn tên gì",
        "nấu ăn như thế nào",
        "máy bay giá bao nhiêu",
        "học lập trình ở đâu",
        "bài hát hay nhất",
        "phim gì hay",
        "công thức nấu phở"
      ],
      "entities": []
    }
  ],
  "entity_patterns": {
    "amount": [
      "(\\d+[\\.]?\\d*)\\s*(k|ngàn|nghìn|triệu|tr|tỷ)",
      "(\\d+[\\.]?\\d*)",
      "một|hai|ba|bốn|năm|sáu|bảy|tám|chín|mười"
    ],
    "target": [
      "cho\\s+(\\w+)",
      "đến\\s+(\\w+)",
      "tên\\s+(\\w+)",
      "username\\s+(\\w+)",
      "tài khoản\\s+(\\w+)"
    ]
  }
}
```

- [ ] **Step 2: Write the Colab training notebook**

Create `python-ai/train.ipynb` as a JSON notebook that:
1. Mounts Google Drive
2. Loads `intents.json` from Drive
3. Tokenizes with `vinai/phobert-base` tokenizer
4. Fine-tunes `vinai/phobert-base` for intent classification (7 labels)
5. Exports model to `phobert-intent.pt`
6. Prints example predictions

The notebook content:

```json
{
 "cells": [
  {
   "cell_type": "code",
   "source": [
    "!pip install transformers torch scikit-learn",
    "import json, torch, numpy as np",
    "from transformers import AutoTokenizer, AutoModelForSequenceClassification, Trainer, TrainingArguments",
    "from sklearn.model_selection import train_test_split",
    "",
    "with open('/content/drive/MyDrive/intents.json') as f:",
    "    data = json.load(f)",
    "",
    "texts, labels, intent_names = [], [], []",
    "for i, intent in enumerate(data['intents']):",
    "    for p in intent['patterns']:",
    "        texts.append(p)",
    "        labels.append(i)",
    "        if i >= len(intent_names):",
    "            intent_names.append(intent['intent'])",
    "",
    "X_train, X_test, y_train, y_test = train_test_split(texts, labels, test_size=0.2, random_state=42)",
    "",
    "tokenizer = AutoTokenizer.from_pretrained('vinai/phobert-base')",
    "train_enc = tokenizer(X_train, truncation=True, padding=True, max_length=64, return_tensors='pt')",
    "test_enc = tokenizer(X_test, truncation=True, padding=True, max_length=64, return_tensors='pt')",
    "",
    "class IntentDataset(torch.utils.data.Dataset):",
    "    def __init__(self, enc, labels):",
    "        self.enc = enc",
    "        self.labels = labels",
    "    def __len__(self): return len(self.labels)",
    "    def __getitem__(self, i):",
    "        return {k: v[i] for k,v in self.enc.items()} | {'labels': torch.tensor(self.labels[i])}",
    "",
    "train_dataset = IntentDataset(train_enc, y_train)",
    "test_dataset = IntentDataset(test_enc, y_test)",
    "",
    "model = AutoModelForSequenceClassification.from_pretrained('vinai/phobert-base', num_labels=len(intent_names))",
    "",
    "training_args = TrainingArguments(",
    "    output_dir='/content/models', num_train_epochs=10, per_device_train_batch_size=8,",
    "    per_device_eval_batch_size=8, eval_strategy='epoch', save_strategy='epoch',",
    "    logging_strategy='epoch', load_best_model_at_end=True, metric_for_best_model='accuracy'",
    ")",
    "",
    "def compute_metrics(eval_pred):",
    "    preds = np.argmax(eval_pred.predictions, axis=1)",
    "    return {'accuracy': (preds == eval_pred.label_ids).mean()}",
    "",
    "trainer = Trainer(model=model, args=training_args, train_dataset=train_dataset,",
    "                  eval_dataset=test_dataset, compute_metrics=compute_metrics)",
    "trainer.train()",
    "",
    "model.save_pretrained('/content/drive/MyDrive/phobert-intent')",
    "torch.save(intent_names, '/content/drive/MyDrive/intent_names.pt')"
   ]
  }
 ]
}
```

- [ ] **Step 3: Commit**

```bash
git add python-ai/data/intents.json python-ai/train.ipynb
git commit -m "feat: add AI datasets and Colab training notebook"
```

---

### Task 2: Python FastAPI server with intent parsing

**Files:**
- Create: `python-ai/requirements.txt`
- Create: `python-ai/model.py`
- Create: `python-ai/main.py`

- [ ] **Step 1: Write requirements.txt**

```
fastapi==0.111.0
uvicorn==0.29.0
torch==2.3.0
transformers==4.41.1
numpy==1.26.4
```

- [ ] **Step 2: Write model.py**

```python
import re, json, torch, numpy as np
from pathlib import Path

MODEL_DIR = Path(__file__).parent / "models"
DATA_DIR = Path(__file__).parent / "data"

class IntentClassifier:
    def __init__(self):
        self.intent_names = torch.load(str(MODEL_DIR / "intent_names.pt"), map_location="cpu", weights_only=False) if (MODEL_DIR / "intent_names.pt").exists() else []
        self.model = None
        self.tokenizer = None
        self.entity_patterns = self._load_entity_patterns()
        self._load_model()

    def _load_entity_patterns(self):
        intents_file = DATA_DIR / "intents.json"
        if not intents_file.exists():
            return {}
        with open(intents_file) as f:
            data = json.load(f)
        return data.get("entity_patterns", {})

    def _load_model(self):
        model_path = MODEL_DIR / "phobert-intent"
        if model_path.exists():
            from transformers import AutoTokenizer, AutoModelForSequenceClassification
            self.tokenizer = AutoTokenizer.from_pretrained("vinai/phobert-base")
            self.model = AutoModelForSequenceClassification.from_pretrained(str(model_path))
            self.model.eval()

    def predict(self, text):
        if self.model is None:
            return self._fallback()
        inputs = self.tokenizer(text, truncation=True, padding=True, max_length=64, return_tensors="pt")
        with torch.no_grad():
            outputs = self.model(**inputs)
            probs = torch.nn.functional.softmax(outputs.logits, dim=-1)
            score, idx = torch.max(probs, dim=-1)
        intent = self.intent_names[idx.item()] if idx.item() < len(self.intent_names) else "fallback"
        if score.item() < 0.3:
            intent = "fallback"
        entities = self._extract_entities(text, intent)
        return {"intent": intent, "confidence": round(score.item(), 2), "entities": entities}

    def _fallback(self):
        return {"intent": "fallback", "confidence": 0.0, "entities": {}}

    def _extract_entities(self, text, intent):
        entities = {}
        for entity, patterns in self.entity_patterns.items():
            for p in patterns:
                m = re.search(p, text, re.IGNORECASE)
                if m:
                    val = m.group(1)
                    if entity == "amount":
                        val = self._parse_amount(val, text)
                    entities[entity] = val
                    break
        return entities

    def _parse_amount(self, val, text):
        multipliers = {"k": 1000, "ngàn": 1000, "nghìn": 1000, "triệu": 1000000, "tr": 1000000, "tỷ": 1000000000}
        text_lower = text.lower()
        for word, mul in multipliers.items():
            if word in text_lower:
                try:
                    num = float(val.replace(".", ""))
                    return int(num * mul)
                except:
                    pass
        try:
            return int(val.replace(".", ""))
        except:
            return 0

classifier = IntentClassifier()
```

- [ ] **Step 3: Write main.py**

```python
from fastapi import FastAPI
from pydantic import BaseModel
from model import classifier

app = FastAPI(title="AI Assistant")

class ParseRequest(BaseModel):
    text: str

class ParseResponse(BaseModel):
    intent: str
    confidence: float
    entities: dict

@app.post("/api/ai/parse", response_model=ParseResponse)
def parse(req: ParseRequest):
    return classifier.predict(req.text)

@app.get("/health")
def health():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=5000)
```

- [ ] **Step 4: Commit**

```bash
git add python-ai/requirements.txt python-ai/model.py python-ai/main.py
git commit -m "feat: add Python AI server with intent classification"
```

---

### Task 3: Java AiService and AiController

**Files:**
- Create: `src/main/java/com/moneytransfer/ai/AiController.java`
- Create: `src/main/java/com/moneytransfer/ai/AiService.java`

- [ ] **Step 1: Write AiService.java**

```java
package com.moneytransfer.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class AiService {

    private final RestTemplate restTemplate;
    private final String pythonUrl;

    public AiService(RestTemplate restTemplate, @Value("${ai.python.url:http://localhost:5000}") String pythonUrl) {
        this.restTemplate = restTemplate;
        this.pythonUrl = pythonUrl;
    }

    public Map<String, Object> parse(String text) {
        String url = pythonUrl + "/api/ai/parse";
        ResponseEntity<Map> response = restTemplate.postForEntity(url, Map.of("text", text), Map.class);
        return response.getBody();
    }
}
```

- [ ] **Step 2: Write AiController.java**

```java
package com.moneytransfer.ai;

import com.moneytransfer.account.AccountService;
import com.moneytransfer.transaction.TransactionService;
import com.moneytransfer.user.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiService aiService;
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final UserService userService;

    public AiController(AiService aiService, TransactionService transactionService,
                        AccountService accountService, UserService userService) {
        this.aiService = aiService;
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.userService = userService;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> chat(Authentication auth, @RequestBody Map<String, String> body) {
        String text = body.get("text");
        if (text == null || text.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Vui lòng nhập câu hỏi"));
        }

        Map<String, Object> parsed = aiService.parse(text);
        String intent = (String) parsed.get("intent");
        Map<String, Object> entities = (Map<String, Object>) parsed.get("entities");
        double confidence = (double) parsed.get("confidence");

        return switch (intent) {
            case "transfer" -> handleTransfer(auth, entities);
            case "balance" -> handleBalance(auth);
            case "history" -> handleHistory(auth);
            case "create_user" -> handleCreateUser(entities);
            case "help" -> ResponseEntity.ok(Map.of("message", helpText()));
            case "greeting" -> ResponseEntity.ok(Map.of("message", "Xin chào! Tôi có thể giúp gì cho bạn?"));
            default -> ResponseEntity.ok(Map.of("message", "Xin lỗi, tôi chưa hiểu ý bạn. Hãy thử: chuyển tiền, xem số dư, xem lịch sử."));
        };
    }

    private ResponseEntity<?> handleTransfer(Authentication auth, Map<String, Object> entities) {
        String username = auth.getName();
        Object targetObj = entities.get("target");
        Object amountObj = entities.get("amount");
        if (targetObj == null) return ResponseEntity.ok(Map.of("message", "Vui lòng cho tôi biết chuyển cho ai?"));
        if (amountObj == null) return ResponseEntity.ok(Map.of("message", "Vui lòng cho tôi biết số tiền?"));
        String target = targetObj.toString();
        int amount = Integer.parseInt(amountObj.toString());
        try {
            transactionService.transfer(username, target, amount, "Chuyển qua AI");
            return ResponseEntity.ok(Map.of("message", "Đã chuyển " + formatVND(amount) + " cho " + target + " thành công"));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Lỗi: " + e.getMessage()));
        }
    }

    private ResponseEntity<?> handleBalance(Authentication auth) {
        String username = auth.getName();
        try {
            var balance = accountService.getTotalBalance(username);
            return ResponseEntity.ok(Map.of("message", "Số dư tài khoản của bạn là " + formatVND(balance)));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Lỗi khi xem số dư"));
        }
    }

    private ResponseEntity<?> handleHistory(Authentication auth) {
        String username = auth.getName();
        try {
            var txs = transactionService.getRecentTransactions(username, 5);
            if (txs.isEmpty()) {
                return ResponseEntity.ok(Map.of("message", "Chưa có giao dịch nào."));
            }
            StringBuilder sb = new StringBuilder("5 giao dịch gần đây:\n");
            for (var tx : txs) {
                sb.append("• ").append(tx.getTransactionCode()).append(": ").append(formatVND(tx.getAmount())).append("\n");
            }
            return ResponseEntity.ok(Map.of("message", sb.toString()));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("message", "Lỗi khi xem lịch sử"));
        }
    }

    private ResponseEntity<?> handleCreateUser(Map<String, Object> entities) {
        Object targetObj = entities.get("target");
        if (targetObj == null) return ResponseEntity.ok(Map.of("message", "Vui lòng cho tôi tên người dùng muốn tạo"));
        return ResponseEntity.ok(Map.of("message", "Chức năng tạo user yêu cầu admin. Liên hệ quản trị viên."));
    }

    private String helpText() {
        return "Tôi có thể giúp bạn:\n" +
               "• Chuyển tiền: \"chuyển 500k cho user2\"\n" +
               "• Xem số dư: \"xem số dư\"\n" +
               "• Lịch sử: \"xem giao dịch gần đây\"\n" +
               "• Hỏi lại câu khác nếu tôi chưa hiểu!";
    }

    private String formatVND(int amount) {
        return String.format("%,d₫", amount);
    }
}
```

- [ ] **Step 3: Add RestTemplate bean in application context**

If `RestTemplate` is not already defined, add to any `@Configuration` class or create a config.

Add to `src/main/java/com/moneytransfer/config/RestTemplateConfig.java`:

```java
package com.moneytransfer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

- [ ] **Step 4: Write test for AiService**

Create `src/test/java/com/moneytransfer/ai/AiServiceTest.java`:

```java
package com.moneytransfer.ai;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AiServiceTest {

    @MockBean
    private RestTemplate restTemplate;

    @Autowired
    private AiService aiService;

    @Test
    void parseTransferIntent() {
        Map<String, Object> mockResponse = Map.of(
            "intent", "transfer",
            "confidence", 0.95,
            "entities", Map.of("amount", 500000, "target", "user2")
        );
        when(restTemplate.postForEntity(anyString(), any(), eq(Map.class)))
                .thenReturn(org.springframework.http.ResponseEntity.ok(mockResponse));

        var result = aiService.parse("chuyển 500k cho user2");
        assertEquals("transfer", result.get("intent"));
        assertEquals(0.95, (double) result.get("confidence"));
    }
}
```

- [ ] **Step 5: Run tests**

Run: `mvn -Dtest=AiServiceTest test`

Expected: BUILD SUCCESS

- [ ] **Step 6: Commit**

```bash
git add src/main/java/com/moneytransfer/ai/ src/main/java/com/moneytransfer/config/RestTemplateConfig.java src/test/java/com/moneytransfer/ai/
git commit -m "feat: add Java AI service and controller"
```

---

### Task 4: Chat Web UI

**Files:**
- Create: `src/main/resources/templates/ai.html`
- Modify: `src/main/resources/static/css/banking.css`
- Modify: `src/main/resources/templates/fragments/layout.html` (sidebar)

- [ ] **Step 1: Create chat page**

Create `src/main/resources/templates/ai.html`:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head th:replace="fragments/layout :: head('Trợ lý AI')"></head>
<body>
    <nav th:replace="fragments/layout :: navbar('ai')"></nav>
    <aside th:replace="fragments/layout :: sidebar('ai')"></aside>
    <main class="main-content">
        <div class="page-header">
            <h1 class="page-title">Trợ lý AI</h1>
            <p class="page-subtitle">Hỏi và thực hiện giao dịch bằng ngôn ngữ tự nhiên</p>
        </div>
        <div class="chat-container">
            <div class="chat-messages" id="chatMessages">
                <div class="chat-msg bot">
                    <div class="chat-msg-content">Xin chào! Tôi là trợ lý AI. Tôi có thể giúp bạn chuyển tiền, xem số dư, xem lịch sử và nhiều hơn thế.</div>
                </div>
            </div>
            <div class="chat-input-area">
                <input type="text" id="chatInput" class="chat-input" placeholder="Nhập câu lệnh...">
                <button id="chatSendBtn" class="btn btn-primary">Gửi</button>
            </div>
        </div>
    </main>
    <div th:replace="fragments/layout :: toast"></div>
    <div th:replace="fragments/layout :: scripts"></div>
    <style>
        .chat-container { max-width: 700px; margin: 0 auto; background: var(--card); border-radius: var(--radius); border: 1px solid var(--border); overflow: hidden; }
        .chat-messages { height: 400px; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 12px; }
        .chat-msg { display: flex; }
        .chat-msg.user { justify-content: flex-end; }
        .chat-msg-content { max-width: 80%; padding: 10px 16px; border-radius: 16px; font-size: 0.9rem; line-height: 1.5; white-space: pre-wrap; }
        .chat-msg.bot .chat-msg-content { background: var(--primary-glow); color: var(--text); border-bottom-left-radius: 4px; }
        .chat-msg.user .chat-msg-content { background: var(--primary); color: #fff; border-bottom-right-radius: 4px; }
        .chat-input-area { display: flex; gap: 8px; padding: 12px 20px; border-top: 1px solid var(--border); background: var(--bg-elevated); }
        .chat-input { flex: 1; padding: 10px 14px; border: 1.5px solid var(--border-light); border-radius: var(--radius-sm); font-size: 0.9rem; outline: none; background: var(--input-bg); color: var(--text); }
        .chat-input:focus { border-color: var(--primary); box-shadow: 0 0 0 3px var(--primary-glow); }
    </style>
    <script>
        document.getElementById('chatSendBtn').addEventListener('click', sendMessage);
        document.getElementById('chatInput').addEventListener('keypress', function(e) { if (e.key === 'Enter') sendMessage(); });

        function sendMessage() {
            var input = document.getElementById('chatInput');
            var text = input.value.trim();
            if (!text) return;

            addMessage(text, 'user');
            input.value = '';
            input.disabled = true;
            document.getElementById('chatSendBtn').disabled = true;

            apiFetch('/api/ai/chat', { method: 'POST', body: { text: text } }).then(function(res) {
                if (res.ok) {
                    addMessage(res.data.message || 'Đã xử lý', 'bot');
                } else {
                    addMessage('Lỗi: ' + (res.data.error || 'Không thể xử lý'), 'bot');
                }
            }).catch(function() {
                addMessage('Máy chủ AI chưa kết nối. Vui lòng khởi động python-ai.', 'bot');
            }).finally(function() {
                input.disabled = false;
                document.getElementById('chatSendBtn').disabled = false;
                input.focus();
            });
        }

        function addMessage(text, role) {
            var container = document.getElementById('chatMessages');
            var div = document.createElement('div');
            div.className = 'chat-msg ' + role;
            div.innerHTML = '<div class="chat-msg-content">' + escapeHtml(text) + '</div>';
            container.appendChild(div);
            container.scrollTop = container.scrollHeight;
        }

        function escapeHtml(text) {
            var d = document.createElement('div');
            d.textContent = text;
            return d.innerHTML;
        }
    </script>
</body>
</html>
```

- [ ] **Step 2: Add sidebar link**

In `src/main/resources/templates/fragments/layout.html`, add a sidebar item for AI before the settings item:

```html
<a th:href="@{/ai}" th:classappend="${currentPage == 'ai'} ? 'active'" class="sidebar-item">
    <span class="icon">🤖</span><span class="sidebar-text" data-i18n="sidebar.ai"> Trợ lý AI</span>
</a>
```

- [ ] **Step 3: Add i18n key**

In `src/main/resources/static/js/translations.js`, add:
```javascript
"sidebar.ai": { vi: "Trợ lý AI", en: "AI Assistant" },
```

- [ ] **Step 4: Verify compilation**

```bash
mvn compile -DskipTests
```

Expected: BUILD SUCCESS

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/templates/ai.html src/main/resources/templates/fragments/layout.html src/main/resources/static/js/translations.js
git commit -m "feat: add AI assistant chat UI"
```

---

### Task 5: Add WebController mapping

**Files:**
- Modify: `src/main/java/com/moneytransfer/WebController.java`

- [ ] **Step 1: Add AI page route**

Add to `WebController.java`:

```java
@GetMapping("/ai")
public String ai() {
    return "ai";
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/com/moneytransfer/WebController.java
git commit -m "feat: add AI page route"
```

---

### Task 6: Verify end-to-end

- [ ] **Step 1: Start Python server**

```bash
cd python-ai
pip install -r requirements.txt
python main.py
```

Note: Need trained model in `python-ai/models/` for full functionality. Without model, server runs but returns fallback intent.

- [ ] **Step 2: Start Java server**

```bash
mvn spring-boot:run
```

- [ ] **Step 3: Test API directly**

```bash
curl -X POST http://localhost:5000/api/ai/parse -H "Content-Type: application/json" -d '{"text":"chuyển 500k cho user2"}'
```

- [ ] **Step 4: Run Java tests**

```bash
mvn -Dtest=AiServiceTest test
```

Expected: BUILD SUCCESS
