# 本地重排服务

RAG 精排阶段使用的交叉编码器服务，模型为 `BAAI/bge-reranker-v2-m3`。

## 为什么是原生进程而不是容器

macOS 上 Docker **无法直通 GPU**：容器跑在 Linux 虚拟机里，CUDA 不存在，Metal 也没有暴露进去。
原生进程可以走 PyTorch 的 MPS 后端吃到 Apple Silicon 的 GPU，同时省掉虚拟机那层内存开销。

fp16 在 MPS 上原生支持，权重内存约 2.3GB 降到 1.1GB，**上下文长度不受影响，仍是 8192 token**。
CPU 上 fp16 通常没有收益甚至更慢，因此非 MPS 环境自动退回 fp32。

有 NVIDIA 卡的部署环境可以改用 `docker compose --profile rerank up -d`（TEI 容器），
后端配置无需改动——两者接口形状一致。

## 启动

```bash
./start.sh
```

首次运行会创建 `.venv` 并下载模型（约 2.3GB，视网络需要几分钟）。

验证：

```bash
curl http://127.0.0.1:8081/health
```

返回中的 `device` 应为 `mps`，`dtype` 应为 `torch.float16`。

## 配置

通过环境变量覆盖：

| 变量 | 默认值 | 说明 |
|---|---|---|
| `RERANK_MODEL` | `BAAI/bge-reranker-v2-m3` | 模型 ID |
| `RERANK_MAX_LENGTH` | `512` | 单条 query+doc 的 token 上限 |
| `RERANK_BATCH_SIZE` | `8` | 推理批大小，显存吃紧可调小 |
| `RERANK_PORT` | `8081` | 监听端口 |

`RERANK_MAX_LENGTH` 说明：模型本身支持 8192，但序列越长激活内存越大。
后端切片上限是 1000 字符（约 600~1000 token），默认 512 会截断超长片段；
如需保留更长上下文，可通过环境变量调回 768 或 1024。
若调大切片，这里要同步调大，否则超出部分会被静默截断。

## 接口

与 Cohere / Jina 的 rerank 接口同形状，后端 `CrossEncoderReranker` 直接对接。

```bash
curl -X POST http://127.0.0.1:8081/rerank \
  -H 'Content-Type: application/json' \
  -d '{"query":"什么是GC","documents":["垃圾回收是...","今天午饭..."],"top_n":1}'
```

```json
{"results": [{"index": 0, "relevance_score": 3.42}]}
```

`relevance_score` 是模型输出的 logit，未归一化，可正可负，只用于排序。

## 关掉重排

后端配置 `app.rag.rerank.provider`：

- `cross-encoder` — 用本服务（默认）
- `llm` — 退回大模型列表式重排，不需要本服务
- `none` — 保持向量原始顺序，用于做 A/B 对照

本服务未启动时后端会在 2 秒超时后自动退回向量顺序，不会导致问答失败。
