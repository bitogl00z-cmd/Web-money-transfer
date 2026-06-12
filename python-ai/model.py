import re, json, os
from pathlib import Path

MODEL_DIR = Path(__file__).parent / "models"
DATA_DIR = Path(__file__).parent / "data"

class IntentClassifier:
    def __init__(self):
        self.intent_names = []
        self.model = None
        self.tokenizer = None
        self.gemini = None
        self.entity_patterns = self._load_entity_patterns()
        self._load_gemini()
        self._load_model()

    def _load_gemini(self):
        key_file = Path(__file__).parent / "gemini_key.txt"
        api_key = os.environ.get("GEMINI_API_KEY")
        if not api_key and key_file.exists():
            api_key = key_file.read_text(encoding="utf-8").strip()
        if api_key:
            try:
                from google import genai
                self.gemini = genai.Client(api_key=api_key)
            except Exception:
                pass

    def _load_entity_patterns(self):
        intents_file = DATA_DIR / "intents.json"
        if not intents_file.exists():
            return {}
        with open(intents_file, encoding="utf-8") as f:
            data = json.load(f)
        return data.get("entity_patterns", {})

    def _load_model(self):
        model_path = MODEL_DIR / "phobert-intent"
        intent_names_file = MODEL_DIR / "intent_names.pt"
        if model_path.exists():
            import torch
            if intent_names_file.exists():
                self.intent_names = torch.load(str(intent_names_file), map_location="cpu", weights_only=False)
            from transformers import AutoTokenizer, AutoModelForSequenceClassification
            self.tokenizer = AutoTokenizer.from_pretrained("vinai/phobert-base")
            self.model = AutoModelForSequenceClassification.from_pretrained(str(model_path))
            self.model.eval()

    def predict(self, text):
        if self.model is None:
            result = self._regex_fallback(text)
            if result["intent"] != "fallback":
                return result
            return self._gemini_fallback(text)
        import torch
        inputs = self.tokenizer(text, truncation=True, padding=True, max_length=64, return_tensors="pt")
        with torch.no_grad():
            outputs = self.model(**inputs)
            probs = torch.nn.functional.softmax(outputs.logits, dim=-1)
            score, idx = torch.max(probs, dim=-1)
        intent = self.intent_names[idx.item()] if idx.item() < len(self.intent_names) else "fallback"
        confidence = round(score.item(), 2)
        if confidence < 0.3:
            result = self._regex_fallback(text)
            if result["intent"] != "fallback":
                return result
            return self._gemini_fallback(text)
        entities = self._extract_entities(text, intent)
        return {"intent": intent, "confidence": confidence, "entities": entities}

    def _regex_fallback(self, text):
        tl = text.lower()
        # Transfer: action verb + number
        if re.search(r'(?:chuy[êe]?n|g[uui]i|chuy[êe]?n\s+kho[aa]?n)\s', tl, re.IGNORECASE):
            entities = self._extract_entities(text, "transfer")
            if entities.get("amount") or entities.get("target"):
                return {"intent": "transfer", "confidence": 0.5, "entities": entities}
        # Balance keywords
        if re.search(r'(?:s[ôo]? d[ưu]|c[òo]n bao nhi[êe]?u|ki[êe]?m tra t[aà]i kho[aà]?n|xem ti[êe]?n|sao k[êe]?)', tl, re.IGNORECASE):
            return {"intent": "balance", "confidence": 0.5, "entities": {}}
        # History keywords
        if re.search(r'(?:giao d[ịi]?ch|l[ịi]?ch s[ưu]?|sao k[êe]?)', tl, re.IGNORECASE):
            return {"intent": "history", "confidence": 0.5, "entities": {}}
        # Greeting
        if re.search(r'^(?:ch[aà]o|hello|hi|helo|hey|xin ch[aà]o)', tl, re.IGNORECASE):
            return {"intent": "greeting", "confidence": 0.5, "entities": {}}
        # Help
        if re.search(r'(?:gi[úu]p|h[ôo]? tr[ơo]?|h[ưú]?[ơo]?ng d[aã]?n|c[óo]? th[êe]? l[aà]m g[iì]|t[ií]?nh n[aă]?ng)', tl, re.IGNORECASE):
            return {"intent": "help", "confidence": 0.5, "entities": {}}
        return self._fallback()

    def _fallback(self):
        return {"intent": "fallback", "confidence": 0.0, "entities": {}}

    def _gemini_fallback(self, text):
        if self.gemini is None:
            return self._fallback()
        try:
            prompt = (
                'Ban la tro ly ngan hang. Phan loai cau sau thanh 1 trong cac intent: '
                'transfer, balance, history, greeting, help.\n'
                'Tra ve JSON thuan (khong markdown, khong giai thich) voi cac field: '
                'intent (string), target (string hoac null), amount (int hoac 0).\n'
                'Vi du: {"intent":"transfer","target":"user2","amount":500000}\n'
                'Cau: "' + text + '"'
            )
            response = self.gemini.models.generate_content(
                model="gemini-2.0-flash", contents=prompt
            )
            raw = response.text.strip()
            raw = raw.removeprefix("```json").removeprefix("```").removesuffix("```").strip()
            data = json.loads(raw)
            intent = data.get("intent", "fallback")
            entities = {}
            target = data.get("target")
            amount = data.get("amount")
            if target:
                entities["target"] = str(target)
            if amount:
                entities["amount"] = self._parse_amount(str(amount), text) if not isinstance(amount, int) else amount
            return {"intent": intent, "confidence": 0.8, "entities": entities}
        except Exception:
            return self._fallback()

    def _extract_entities(self, text, intent):
        entities = {}
        tl = text.lower()
        # === Extract AMOUNT: find any number with optional unit ===
        amount_match = re.search(
            r'(\d+[\.,]?\d*)\s*(k|ng[aà]?n|ngh[iì]?n|tri[eệ]u|tr|t[ỷi]?)\b|(\d{4,})',
            tl, re.IGNORECASE
        )
        if amount_match:
            val = amount_match.group(1) or amount_match.group(3)
            unit = (amount_match.group(2) or "").lower()
            if unit:
                entities["amount"] = self._parse_amount(val, text, unit)
            else:
                entities["amount"] = self._parse_amount(val, text, "")
        # === Extract TARGET: find username-like word ===
        # Pattern 1: after cho/den/den/tai khoan
        prep_match = re.search(
            r'(?:cho|đến|den|t[aà]i kho[aà]?n|t[êe]?n|username)\s+([a-zA-Z0-9_]+(?:\s+[a-zA-Z0-9_]+)*)',
            tl, re.IGNORECASE
        )
        if prep_match:
            target = prep_match.group(1).strip()
            if re.search(r'\d', target):  # has digit → likely username
                entities["target"] = target
            elif "target" not in entities and target:
                entities["target"] = target
        # Pattern 2: between action verb and amount (no preposition)
        if intent == "transfer" and "target" not in entities and "amount" in entities:
            action = re.search(r'(?:chuy[êe]?n|g[ưu]i)\s+(?:kho[aà]?n\s+)?(?:b[aạ]?n\s+)?(.+?)\s+\d', tl, re.IGNORECASE)
            if action:
                possible = action.group(1).strip()
                if re.search(r'[a-zA-Z]', possible) and possible != 'tiền':
                    # Take last word that has letters
                    words = [w for w in possible.split() if re.search(r'[a-zA-Z]', w)]
                    if words:
                        entities["target"] = words[-1]
        # Pattern 3: last alphanumeric word in the text (for transfer)
        if intent == "transfer" and "target" not in entities:
            words = re.findall(r'[a-zA-Z]\w+', tl)
            # Exclude amount-related words, take the last one
            skip = {'k', 'ngàn', 'nghìn', 'triệu', 'tr', 'tỷ', 'chuyển', 'gửi', 'khoản', 'bạn', 'cho', 'đến', 'tiền'}
            for w in reversed(words):
                if w not in skip and len(w) >= 2:
                    entities["target"] = w
                    break
        return entities

    def _parse_amount(self, val, text, unit=""):
        multipliers = {"k": 1000, "ngàn": 1000, "nghìn": 1000, "triệu": 1000000, "tr": 1000000, "tỷ": 1000000000}
        if not unit:
            # auto-detect from text
            text_lower = text.lower()
            for word, mul in multipliers.items():
                if word in text_lower:
                    unit = word
                    break
        mul = multipliers.get(unit, 1)
        try:
            num = float(val.replace(".", "").replace(",", ""))
            return int(num * mul)
        except:
            return 0

classifier = IntentClassifier()
