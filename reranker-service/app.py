"""
本地重排服务 (bge-reranker-v2-m3)

对外暴露与 Cohere / Jina 同形状的 /rerank 接口，因此后端 Java 侧
只需把 app.rag.rerank.base-url 指向本服务，无需改动任何代码。

选择原生进程而非 Docker 容器的原因：
macOS 上 Docker 无法直通 GPU，容器内只能跑 CPU；原生进程可以走 PyTorch 的
MPS 后端吃到 Apple Silicon 的 GPU，并省掉虚拟机那一层内存开销。

fp16 在 MPS 上是原生支持的，权重内存相比 fp32 减半（约 2.3GB -> 1.1GB），
且上下文长度不受影响，仍为 8192 token。CPU 上 fp16 通常没有收益甚至更慢，
因此非 MPS 环境自动退回 fp32。
"""

import logging
import os
import time
from contextlib import asynccontextmanager
from typing import List, Optional

import torch
import uvicorn
from fastapi import FastAPI
from pydantic import BaseModel, Field
from transformers import AutoModelForSequenceClassification, AutoTokenizer

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
)
log = logging.getLogger("reranker")

MODEL_ID = os.getenv("RERANK_MODEL", "BAAI/bge-reranker-v2-m3")
# 切片上限 1000 字符约合 600~1000 token；默认 512 优先控制关键路径延迟。
# 模型本身支持到 8192，但序列越长激活内存越大，按实际需要设定即可。
MAX_LENGTH = int(os.getenv("RERANK_MAX_LENGTH", "512"))
# 分批推理，避免一次性构造过大的注意力中间张量
BATCH_SIZE = int(os.getenv("RERANK_BATCH_SIZE", "8"))
PORT = int(os.getenv("RERANK_PORT", "8081"))


def pick_device() -> tuple[str, torch.dtype]:
    """选择推理设备与精度。fp16 只在 MPS/CUDA 上启用。"""
    if torch.backends.mps.is_available():
        return "mps", torch.float16
    if torch.cuda.is_available():
        return "cuda", torch.float16
    return "cpu", torch.float32


class RerankRequest(BaseModel):
    query: str
    # documents 为托管 API 的字段名，texts 为 TEI 的字段名，两者都接受
    documents: Optional[List[str]] = None
    texts: Optional[List[str]] = None
    top_n: Optional[int] = Field(default=None, ge=1)
    model: Optional[str] = None

    def candidates(self) -> List[str]:
        return self.documents or self.texts or []


class RerankResult(BaseModel):
    index: int
    relevance_score: float


class RerankResponse(BaseModel):
    results: List[RerankResult]


state: dict = {}


@asynccontextmanager
async def lifespan(_: FastAPI):
    device, dtype = pick_device()
    log.info("加载模型 %s (device=%s, dtype=%s)", MODEL_ID, device, dtype)

    started = time.time()
    tokenizer = AutoTokenizer.from_pretrained(MODEL_ID)
    model = AutoModelForSequenceClassification.from_pretrained(MODEL_ID, torch_dtype=dtype)
    model.to(device)
    model.eval()

    state.update(tokenizer=tokenizer, model=model, device=device, dtype=str(dtype))
    log.info("模型就绪，耗时 %.1fs", time.time() - started)
    yield
    state.clear()


app = FastAPI(title="KnowledgeFlow Reranker", lifespan=lifespan)


@app.get("/health")
def health():
    return {
        "status": "ok" if "model" in state else "loading",
        "model": MODEL_ID,
        "device": state.get("device"),
        "dtype": state.get("dtype"),
        "max_length": MAX_LENGTH,
    }


@torch.inference_mode()
def score_pairs(query: str, candidates: List[str]) -> List[float]:
    """逐批计算 query 与每个候选的相关性分数"""
    tokenizer = state["tokenizer"]
    model = state["model"]
    device = state["device"]

    scores: List[float] = []
    for start in range(0, len(candidates), BATCH_SIZE):
        batch = candidates[start:start + BATCH_SIZE]
        inputs = tokenizer(
            [[query, doc] for doc in batch],
            padding=True,
            truncation=True,
            max_length=MAX_LENGTH,
            return_tensors="pt",
        ).to(device)

        logits = model(**inputs).logits.view(-1)
        # fp16 结果转回 float32 再出参，避免序列化精度问题
        scores.extend(logits.float().cpu().tolist())

    return scores


@app.post("/rerank", response_model=RerankResponse)
def rerank(req: RerankRequest):
    candidates = req.candidates()
    if not candidates:
        return RerankResponse(results=[])

    started = time.time()
    scores = score_pairs(req.query, candidates)

    ranked = sorted(enumerate(scores), key=lambda pair: pair[1], reverse=True)
    if req.top_n:
        ranked = ranked[: req.top_n]

    log.info(
        "重排完成: candidates=%d, top_n=%s, 耗时 %.0fms",
        len(candidates), req.top_n, (time.time() - started) * 1000,
    )

    return RerankResponse(
        results=[RerankResult(index=i, relevance_score=s) for i, s in ranked]
    )


if __name__ == "__main__":
    uvicorn.run(app, host="127.0.0.1", port=PORT)
