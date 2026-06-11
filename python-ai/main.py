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
