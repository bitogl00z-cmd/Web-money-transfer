import re, json
from pathlib import Path

MODEL_DIR = Path(__file__).parent / "models"
DATA_DIR = Path(__file__).parent / "data"

class IntentClassifier:
    def __init__(self):
        self.intent_names = []
        self.model = None
        self.tokenizer = None
        self.entity_patterns = self._load_entity_patterns()
        self._load_model()

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
            return self._fallback()
        import torch
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
