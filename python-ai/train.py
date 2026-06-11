import json, torch, numpy as np
from transformers import AutoTokenizer, AutoModelForSequenceClassification, Trainer, TrainingArguments
from sklearn.model_selection import train_test_split

with open('data/intents.json') as f:
    data = json.load(f)

texts, labels, intent_names = [], [], []
for i, intent in enumerate(data['intents']):
    for p in intent['patterns']:
        texts.append(p)
        labels.append(i)
        if i >= len(intent_names):
            intent_names.append(intent['intent'])

X_train, X_test, y_train, y_test = train_test_split(texts, labels, test_size=0.2, random_state=42)

tokenizer = AutoTokenizer.from_pretrained('vinai/phobert-base')
train_enc = tokenizer(X_train, truncation=True, padding=True, max_length=64, return_tensors='pt')
test_enc = tokenizer(X_test, truncation=True, padding=True, max_length=64, return_tensors='pt')

class IntentDataset(torch.utils.data.Dataset):
    def __init__(self, enc, labels):
        self.enc = enc
        self.labels = labels
    def __len__(self): return len(self.labels)
    def __getitem__(self, i):
        return {k: v[i] for k,v in self.enc.items()} | {'labels': torch.tensor(self.labels[i])}

train_dataset = IntentDataset(train_enc, y_train)
test_dataset = IntentDataset(test_enc, y_test)

model = AutoModelForSequenceClassification.from_pretrained('vinai/phobert-base', num_labels=len(intent_names))

training_args = TrainingArguments(
    output_dir='models', num_train_epochs=10, per_device_train_batch_size=8,
    per_device_eval_batch_size=8, eval_strategy='epoch', save_strategy='epoch',
    logging_strategy='epoch', load_best_model_at_end=True, metric_for_best_model='accuracy',
)

def compute_metrics(eval_pred):
    preds = np.argmax(eval_pred.predictions, axis=1)
    return {'accuracy': (preds == eval_pred.label_ids).mean()}

trainer = Trainer(model=model, args=training_args, train_dataset=train_dataset,
                  eval_dataset=test_dataset, compute_metrics=compute_metrics)
trainer.train()

model.save_pretrained('models/phobert-intent')
torch.save(intent_names, 'models/intent_names.pt')

test_input = tokenizer(["chuyển 500k cho user2", "xem số dư"], truncation=True, padding=True, return_tensors='pt')
outputs = model(**test_input)
preds = torch.argmax(outputs.logits, dim=-1)
for text, pred in zip(["chuyển 500k cho user2", "xem số dư"], preds):
    print(f"'{text}' → {intent_names[pred.item()]}")
