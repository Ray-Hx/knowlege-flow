# KnowledgeFlow 教学向技术文档 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 产出 7 篇教学向技术文档，覆盖 KnowledgeFlow 的 RAG 链路、混合检索、异步一致性与间隔重复算法，每篇能独立支撑 30 分钟深度追问。

**Architecture:** 每篇文档遵循固定六段式（问题场景 → 原理推导 → 代码走读 → 选型对比 → 追问预演 → 已知缺陷）。文档以代码为唯一事实来源；本计划中所有代码论断与行号均已预先核实，任务执行时须重新核对。

**Tech Stack:** Markdown。无构建、无测试框架。本计划中「测试」= 事实核验脚本（grep / sed 断言代码仍与文档论断一致）。

## Global Constraints

以下约束适用于每一个任务，不再逐任务重复：

- 文档输出目录固定为 `docs/tech/`，文件名与本计划一致，不得改名。
- **代码是唯一事实来源。** 与 `README.md` / `TECHNICAL_DETAILS.md` / `README_Graduation_Defense.md` 冲突时以代码为准。
- **禁止编造性能数字。** 未实测的延迟、吞吐、准确率一律不写；确需提及写「未实测」。
- **禁止编造决策历史。** 不写「我当时对比了 N 种方案」；事后补充的分析必须标注为事后分析。
- **禁止营销词汇**：颠覆性、极致、业界领先、完美、深度赋能。
- 每个 `file:line` 引用必须在写作时用 `sed -n 'N,Mp'` 实际打开确认。
- 单个代码片段不超过 30 行；更长的用 `文件:起-止` 指路。
- 技术术语首次出现中英并列（如「交叉编码器（cross-encoder）」），其后统一用中文。
- 每篇第 ⑥ 段至少一条**具体**缺陷，附 `file:line`。不接受「可以进一步优化」这类空话。
- 六段的标题文字固定为：`## 一、这个问题为什么难` / `## 二、原理` / `## 三、代码走读` / `## 四、为什么不用 X 而用 Y` / `## 五、追问预演` / `## 六、现有实现的坑与改进方向`。

---

### Task 0: 建立目录与事实核验脚本

**Files:**
- Create: `docs/tech/.gitkeep`
- Create: `scripts/verify-docs.sh`

**Interfaces:**
- Produces: `scripts/verify-docs.sh` — 后续每个任务的「测试」步骤都调用它。断言集以 `ASSERTIONS` 数组形式追加，每条是 `文件:期望出现的字符串`。

本任务先建立核验机制，后续每篇文档写完都用它验一遍论断没有失效。

- [ ] **Step 1: 写核验脚本（此时应当失败，因为断言的文档还不存在）**

创建 `scripts/verify-docs.sh`：

```bash
#!/usr/bin/env bash
# 教学文档事实核验：确认文档中的代码论断与当前代码一致。
# 用法: ./scripts/verify-docs.sh
set -uo pipefail
cd "$(dirname "$0")/.."

fail=0
pass=0

# 断言代码中确实存在某字符串。用于保证文档论断没有随代码漂移。
assert_code() {
  local file="$1" pattern="$2" desc="$3"
  if [[ ! -f "$file" ]]; then
    echo "  FAIL  $desc — 文件不存在: $file"; fail=$((fail+1)); return
  fi
  if grep -qF -- "$pattern" "$file"; then
    echo "  ok    $desc"; pass=$((pass+1))
  else
    echo "  FAIL  $desc — 在 $file 中找不到: $pattern"; fail=$((fail+1))
  fi
}

# 断言文档存在且六段齐全、无占位符。
assert_doc() {
  local doc="$1"
  if [[ ! -f "$doc" ]]; then
    echo "  FAIL  文档不存在: $doc"; fail=$((fail+1)); return
  fi
  local missing=""
  for h in "一、这个问题为什么难" "二、原理" "三、代码走读" \
           "四、为什么不用" "五、追问预演" "六、现有实现的坑"; do
    grep -qF -- "$h" "$doc" || missing="$missing [$h]"
  done
  if [[ -n "$missing" ]]; then
    echo "  FAIL  $doc 缺少段落:$missing"; fail=$((fail+1)); return
  fi
  if grep -qE 'TODO|TBD|待补充|XXX' "$doc"; then
    echo "  FAIL  $doc 含占位符"; fail=$((fail+1)); return
  fi
  if grep -qE '颠覆性|极致|业界领先|深度赋能' "$doc"; then
    echo "  FAIL  $doc 含营销词汇"; fail=$((fail+1)); return
  fi
  echo "  ok    $doc 结构完整"; pass=$((pass+1))
}

echo "== 代码论断核验 =="
# 断言随任务逐个追加到此处。

echo "== 文档结构核验 =="
# 断言随任务逐个追加到此处。

echo
echo "通过 $pass 项，失败 $fail 项"
[[ $fail -eq 0 ]]
```

- [ ] **Step 2: 赋予执行权限并运行，确认脚本本身可跑**

```bash
mkdir -p docs/tech && touch docs/tech/.gitkeep
chmod +x scripts/verify-docs.sh
./scripts/verify-docs.sh
```

预期输出：两个标题行 + `通过 0 项，失败 0 项`，退出码 0。

- [ ] **Step 3: 提交**

```bash
git add scripts/verify-docs.sh docs/tech/.gitkeep
git commit -m "docs: add teaching-doc fact verification script"
```

---

### Task 1: 01-分块策略

**Files:**
- Create: `docs/tech/01-分块策略-从固定长度到语义切分.md`
- Modify: `scripts/verify-docs.sh`（追加本篇断言）
- Read-only 素材: `src/main/java/com/ray/knowledgeflow/module/ai/service/SemanticChunker.java`（259 行）、`ChunkingService.java`（285 行）

**Interfaces:**
- Produces: 本篇确立六段式的行文风格与深度基准，后续 6 篇对齐本篇。特别是「② 原理」段从零推导的详略程度。
- Consumes: Task 0 的 `scripts/verify-docs.sh`。

**已核实的事实（写作依据，须重新核对行号）:**

| 事实 | 位置 |
| :--- | :--- |
| 句子边界正则，英文句点要求其后为空白，避免切开 `3.14` / `Node.js` | `SemanticChunker.java:53` 附近 `SENTENCE_END` |
| Markdown 标题行强制作为语义边界 | `SemanticChunker.java:56` 附近 `HEADING` |
| 句子数 < 3 时直接按长度收尾（无从比较语义距离） | `chunk()` 方法内 |
| 嵌入数量与句子数不符时降级为定长切分 | `chunk()` 内 `fixedSizeFallback` |
| 邻域拼接（`bufferSize`，默认 1）抑制单句噪声 | `buildWindows()` |
| 余弦距离 = 1 − 余弦相似度，取值 [0, 2] | `cosineDistance()` |
| 阈值取**分位数**（默认第 85 百分位）而非绝对值 | `percentile()`，配置 `app.rag.chunk.breakpoint-percentile:85` |
| 分位数用线性插值实现 | `percentile()` |
| 切分后收尾：短片合并、超长硬拆 | `enforceSizeLimits()` |
| 默认 `min-chars:80`、`max-chars:1000` | 字段 `@Value` 默认值 |
| 静态纯函数便于单测，含 `configure()` 测试注入方法 | 文件末尾 |
| 向量维度 2048 | `ChunkingService.java:36`、`EmbeddingService.java:111` |
| 嵌入前把标题/摘要/标签拼进待嵌入文本，但 payload 不变 | `ChunkingService.indexContent()` |

- [ ] **Step 1: 核对素材行号**

```bash
sed -n '45,75p'   src/main/java/com/ray/knowledgeflow/module/ai/service/SemanticChunker.java
sed -n '95,135p'  src/main/java/com/ray/knowledgeflow/module/ai/service/SemanticChunker.java
sed -n '165,215p' src/main/java/com/ray/knowledgeflow/module/ai/service/SemanticChunker.java
sed -n '30,70p'   src/main/java/com/ray/knowledgeflow/module/ai/service/ChunkingService.java
```

把实际行号记下来，写作时用真实行号，不要沿用本计划中的约数。

- [ ] **Step 2: 写文档**

创建 `docs/tech/01-分块策略-从固定长度到语义切分.md`，六段内容要点如下。

**① 这个问题为什么难** — 从具体失败场景切入，不要从「什么是分块」切入。用这个例子：

> 一篇讲 RRF 的笔记里写着「……RRF 通过排名而非分数融合。常数 k 取 60，来自 Cormack 等人 2009 年的实验。」定长切分在 500 字处断开，「常数 k 取 60」落在 chunk A 末尾，「来自 Cormack……」进了 chunk B。用户问「RRF 的 k 为什么是 60」，向量检索命中 chunk A——检索「成功」了，但答案在 chunk B 里。

点明矛盾：切片要够小（嵌入模型有长度上限、太长会稀释语义），又要够完整（切碎了每片都不足以回答问题）。定长切分对这对矛盾完全无感知，因为它只看字符数、不看语义。

**② 原理** — 按以下顺序推导，每步说明「为什么需要这一步」：

1. 把文本切成句子——语义边界一定落在句子边界上，先把候选边界集合缩小。
2. 为每个句子求向量——但**单句向量噪声大**，「所以呢？」这种句子的向量几乎不携带主题信息。因此改为求「句子 i 及其前后各 `bufferSize` 句拼接后」的向量（滑动窗口），用邻域平滑掉单句噪声。
3. 计算相邻窗口的余弦距离。解释余弦距离 = 1 − 余弦相似度，取值范围 [0, 2]，越大表示语义差异越大。说明为什么用余弦而非欧氏（嵌入向量关心方向不关心模长）。
4. **关键一步：阈值怎么定。** 这是本篇的核心。先说绝对阈值的失效：不同文档的向量距离分布差异极大——一篇术语密集的技术笔记里相邻句距离普遍偏大，一篇叙述连贯的散文里普遍偏小。同一个绝对阈值 0.3，在前者切得过碎，在后者一刀不切。改用**分位数**：取所有相邻距离的第 85 百分位作阈值，等价于「不管这篇文档整体的距离水平如何，都在最突兀的那 15% 处断开」。这把阈值从绝对量变成了**相对于本文档分布的相对量**，因此对文档风格免疫。
5. 收尾：语义边界不保证长度均匀，短片合并、超长硬拆。

配一个 ASCII 图示，横轴句子序号、纵轴相邻距离、一条水平虚线表示分位数阈值、峰值处标出切分点。

**③ 代码走读** — 按上面五步顺序，每步贴对应片段（用 Step 1 核对的真实行号）：

- `SENTENCE_END` 与 `HEADING` 正则（约 5 行）：重点讲 `(?<=\.)(?=\s)` 这个先行断言为什么必要——没有它 `Node.js`、`3.14` 会被切开。
- `buildWindows()`（约 12 行）：邻域拼接。
- `adjacentDistances()` + `cosineDistance()`（约 15 行）：注意 `normA == 0` 的保护。
- `percentile()`（约 15 行）：线性插值，讲清 `rank = (p/100) * (size-1)` 为什么是 `size-1`。
- `assemble()`（约 15 行）：`semanticBreak || tooLong` 双条件。
- 降级路径：`sentences.size() < 3` 与 `embeddings.size() != sentences.size()` 两处 fallback，强调**索引链路不能因为嵌入服务抖动而中断**。

**④ 为什么不用 X 而用 Y** — 三组对比，每组给出适用场景而非简单否定：

| 方案 | 优点 | 为什么本项目没用 |
| :--- | :--- | :--- |
| 定长 + 重叠窗口（如 512 字符、overlap 50） | 实现简单、零额外成本、完全可预测 | 重叠只能缓解截断不能消除；重叠部分重复入库，膨胀索引且同一内容重复召回 |
| 按 Markdown 结构切（标题层级） | 零嵌入成本、边界天然合理 | 依赖作者的标题习惯；一个二级标题下写 3000 字就退化成超长片。**注意：本项目实际保留了这一策略的一半**——标题行强制成为语义边界 |
| 语义切分（本项目） | 对文档风格免疫、边界贴合语义 | 代价是每次分块要为每个句子求向量，成本见 ⑥ |

诚实标注：定长+重叠是**当前 RAG 工程的主流默认选择**，语义切分并非普遍更优，它在结构松散的长文本上收益最明显。

**⑤ 追问预演** — 三层追问，每层给可直接说出口的回答：

- 第一层：「为什么百分位取 85 而不是别的？」
  → 它控制切片粒度：越大切得越少、片越长。85 是经验起点不是推导结果，配置项 `app.rag.chunk.breakpoint-percentile` 可调。**明确承认没有跑过评测集调参**——这是当前实现的短板，不要硬编一个「我测过 70/85/95」的说法。
- 第二层：「分位数阈值有个隐患——如果整篇文档语义高度连贯，最大的距离也很小，你还是会在第 85 百分位处切一刀。这合理吗？」
  → 承认这是真实缺陷。分位数保证了「总会切」，即使文档根本不该切。缓解手段是叠加绝对下限（距离低于某值不切），当前实现没有做。这是可以主动提出的改进点。
- 第三层：「每个句子都要求一次向量，一篇 5000 字的笔记大概 100 多个句子，就是 100 多次嵌入调用。索引一篇笔记的延迟你能接受吗？」
  → 见 ⑥。承认无缓存、无批量去重。`embedAll` 是批量接口这一点要讲清（不是 100 次 HTTP 往返），但嵌入 token 成本仍与句子数成正比。

**⑥ 现有实现的坑与改进方向** — 至少覆盖：

1. **无嵌入缓存。** 编辑笔记后重新索引，未改动的段落也要重新求向量。改进：按句子哈希缓存向量。
2. **分位数保证「总会切」。** 见 ⑤ 第二层，缺绝对下限保护。
3. **`enforceSizeLimits` 的硬拆是按字符暴力截断**（`chunk.substring(i, i+maxChars)`），超长片被拆开时又回到了本篇开篇批评的定长切分问题。
4. **`ChunkingService` 与 `SemanticChunker` 的向量维度 2048 硬编码在两处**（`ChunkingService.java:36`、`EmbeddingService.java:111`），换嵌入模型要改两个地方。
5. 顺带澄清：`README_Graduation_Defense.md` 称向量为 1536 维，与代码不符，实际 2048。

- [ ] **Step 3: 追加断言到核验脚本**

在 `scripts/verify-docs.sh` 的 `== 代码论断核验 ==` 段后追加：

```bash
CHUNKER=src/main/java/com/ray/knowledgeflow/module/ai/service/SemanticChunker.java
assert_code "$CHUNKER" 'breakpoint-percentile:85' "01: 分位数默认值仍为 85"
assert_code "$CHUNKER" 'min-chars:80'             "01: minChars 默认值仍为 80"
assert_code "$CHUNKER" 'max-chars:1000'           "01: maxChars 默认值仍为 1000"
assert_code "$CHUNKER" '1.0 - dot / (Math.sqrt(normA) * Math.sqrt(normB))' "01: 余弦距离公式未变"
assert_code "$CHUNKER" 'fixedSizeFallback'        "01: 定长降级路径仍存在"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/ChunkingService.java \
            'createCollection(CHUNK_COLLECTION, 2048)' "01: 向量维度仍为 2048"
```

在 `== 文档结构核验 ==` 段后追加：

```bash
assert_doc "docs/tech/01-分块策略-从固定长度到语义切分.md"
```

- [ ] **Step 4: 运行核验，确认全部通过**

```bash
./scripts/verify-docs.sh
```

预期：7 项 ok，`通过 7 项，失败 0 项`，退出码 0。
若某条 FAIL，说明计划中的事实已过期——**以代码为准修改文档**，不要改断言去迁就文档。

- [ ] **Step 5: 提交**

```bash
git add docs/tech/01-分块策略-从固定长度到语义切分.md scripts/verify-docs.sh
git commit -m "docs: add teaching doc on semantic chunking strategy"
```

---

### Task 2: 02-召回精排两段式检索

**Files:**
- Create: `docs/tech/02-召回精排两段式检索.md`
- Modify: `scripts/verify-docs.sh`
- Read-only 素材: `RagRetriever.java`（191 行）、`Reranker.java`（39 行）

**Interfaces:**
- Consumes: Task 1 确立的行文风格；本篇 ① 段可直接引用 01 篇的分块结论（切片已经切好了，接下来是怎么取）。
- Produces: 「重排器是可替换策略」这一结论，Task 3 在此基础上讲三种实现。本篇只讲**为什么要重排**，不讲**怎么实现重排**，避免与 Task 3 重复。

**已核实的事实:**

| 事实 | 位置 |
| :--- | :--- |
| 召回候选数 `candidate-size` 默认 20，精排后 `top-k` 默认 5 | `RagRetriever.java` 字段 `@Value` |
| 向量相似度下限 `score-threshold` 默认 0.5 | 同上 |
| 集合名 `content_chunks` | `RagRetriever.CHUNK_COLLECTION` |
| Qdrant 查询带 `userId` 过滤，防止检索到他人笔记 | `userFilter()` |
| 检索失败降级为无上下文原始 prompt，不阻断问答 | `retrieve()` 的 catch 块 + `RagContext.empty()` |
| `InterruptedException` 单独捕获并恢复中断标志 | `retrieve()` |
| 拼装时按 `sourceId` 去重引用列表（`addedSourceIds`） | `build()` |
| 引用摘要截断至 100 字符 | `build()` |
| 注释明确：检索逻辑原先在同步问答和流式问答各写一遍，此处统一 | `RagRetriever` 类注释 |
| `Reranker.naturalOrder()` 是接口静态方法，所有实现失败时退回它 | `Reranker.java` |

- [ ] **Step 1: 核对素材行号**

```bash
sed -n '18,55p'   src/main/java/com/ray/knowledgeflow/module/ai/service/RagRetriever.java
sed -n '76,130p'  src/main/java/com/ray/knowledgeflow/module/ai/service/RagRetriever.java
sed -n '165,191p' src/main/java/com/ray/knowledgeflow/module/ai/service/RagRetriever.java
cat src/main/java/com/ray/knowledgeflow/module/ai/service/Reranker.java
```

- [ ] **Step 2: 写文档**

**① 这个问题为什么难** — 场景：

> 用户问「Qdrant 的连接超时怎么配」。向量检索返回相似度最高的 5 个片段，其中 3 个来自一篇讲 Qdrant 集合创建的笔记——它们和问题用词高度重合（Qdrant、连接、配置），余弦相似度很高，但都没提超时。真正写了超时配置的那一段，因为用词是「timeout 参数」而非「连接超时」，排在第 8 位，被 top-5 截断在外。

点明核心矛盾：**相似 ≠ 能回答**。向量检索是在「语义邻近」这个代理指标上做排序，而我们真正想要的是「相关性」。两者相关但不等价。

**② 原理** — 讲清 bi-encoder 与 cross-encoder 的结构差异，这是本篇的理论核心：

- **bi-encoder（双编码器）**：query 和 document 各自独立过一次模型，得到两个向量，再算余弦。关键性质是 **document 向量可以预计算并建索引**——这正是向量检索能在百万级语料上做到毫秒响应的原因。代价：query 和 document 在编码时**互相看不见**，模型无法针对这个具体问题去判断这个文档有没有回答它。
- **cross-encoder（交叉编码器）**：query 和 document 拼成一个序列过同一个模型，注意力层让两者逐 token 交互，输出一个相关性分数。精度显著更高。代价：**无法预计算**——分数依赖具体的 query，N 个文档就要跑 N 次前向。
- 因此二者不是替代关系而是**级联关系**：bi-encoder 负责从百万级里快速捞出几十个（保召回率），cross-encoder 负责在这几十个里精排（保精确率）。这就是「召回—精排」两段式。

再解释 `candidateSize=20` 与 `topK=5` 这组数字的约束关系：候选数必须**明显大于** topK 才有精排空间（若候选 = 5、topK = 5，精排只能改变顺序，无法改变集合成分，收益接近零）；但候选数受 cross-encoder 延迟约束，20 个候选就是 20 次前向。

配一个两段式流程图（ASCII）：`query → embed → Qdrant top-20（含 userId 过滤、score≥0.5）→ reranker → top-5 → 拼 prompt`。

**③ 代码走读**（用 Step 1 的真实行号）：

- 三个 `@Value` 配置字段（约 10 行）：重点是注释「召回阶段候选数，需明显大于 topK 才有精排空间」。
- `retrieve()` 的召回段（约 12 行）：`setLimit(candidateSize)` + `setFilter(userFilter(userId))` + `setScoreThreshold(scoreThreshold)`。强调 `userFilter` 是**在向量库侧过滤**而非取回后过滤——后者会让 topK 名额被他人笔记占掉。
- `retrieve()` 的精排段（约 6 行）：把候选文本抽出来交给 `reranker.rerank(query, texts, topK)`，拿回的是**下标列表**而非文本。讲清返回下标的设计好处：重排器不需要知道 Qdrant 的数据结构，只处理字符串，接口边界干净、易于单测。
- 三处降级（约 12 行）：`vector.isEmpty()`、`candidates.isEmpty()`、catch 块 → 全部走 `RagContext.empty(query)`，即降级为**不带上下文的原始提问**。强调这个设计取向：RAG 失败时宁可让模型用通用知识回答，也不能让用户看到报错。
- `Reranker.naturalOrder()`（约 8 行）：接口里放静态默认实现，保证所有实现的降级行为一致。

**④ 为什么不用 X 而用 Y**：

| 方案 | 为什么不用 |
| :--- | :--- |
| 只用向量 top-5，不做重排 | 就是 ① 里的失败场景。**但要诚实说明**：这是绝大多数入门 RAG 的做法，在语料小、问题简单时够用 |
| 加大 topK 到 20，全部塞进 prompt | 上下文窗口和成本都涨；更重要的是「迷失在中间」（lost in the middle）——长上下文里模型对中段内容的利用率显著下降，塞更多不等于答得更好 |
| 用 cross-encoder 直接扫全库 | 无法预计算，百万级语料每次查询要跑百万次前向，完全不可行 |
| 在 Qdrant 侧调高 `score_threshold` 来提精度 | 阈值卡的是余弦相似度，而 ① 的问题恰恰是「相似度高但不相关」，调阈值治不了这个病 |

**⑤ 追问预演**：

- 第一层：「候选 20、精排 5，这两个数怎么定的？」
  → 讲约束关系（候选须远大于 topK；候选数受重排延迟约束）。**承认 20/5 是经验值，没有跑评测集调参。**
- 第二层：「`score-threshold` 设了 0.5，这会不会把正确答案在召回阶段就滤掉了？」
  → 会。阈值是精确率与召回率的直接权衡，卡在召回阶段的损失**重排救不回来**。两段式的前提是召回阶段尽量宽。0.5 偏保守，且不同嵌入模型的相似度分布不同，这个绝对阈值换模型后未必仍合适——与 01 篇「绝对阈值不如相对阈值」是同一类问题。
- 第三层：「你的 `userFilter` 是在 Qdrant 侧过滤的。那 payload 里的 `userId` 字段建索引了吗？没建索引的话 Qdrant 是先做向量检索再过滤，还是先过滤再检索？这会影响你实际拿到的候选数量。」
  → **这是本篇最该老实承认的一处。** 代码只调了 `setFilter`，没有显式创建 payload 索引。Qdrant 对未建索引的 payload 字段做过滤时执行效率和召回行为都可能不同于预期，实际拿回的候选数可能少于 20。**没有验证过这一点，应当承认并列为改进项。**

**⑥ 现有实现的坑与改进方向**：

1. **`userId` payload 字段未显式建索引**（见 ⑤ 第三层），可能影响实际召回数量。
2. **`score-threshold` 是绝对阈值**，换嵌入模型后需重新标定。
3. **候选数与 topK 均未经评测集验证**，是经验值。
4. **`build()` 按 `sourceId` 去重引用列表，但 prompt 里仍会拼入同一笔记的多个片段**——引用展示去重了，上下文没去重。同一篇笔记的 3 个片段会占掉 3 个 topK 名额，降低来源多样性。
5. **无检索结果缓存**，同一问题重复提问会重复执行完整链路。

- [ ] **Step 3: 追加断言**

```bash
RAG=src/main/java/com/ray/knowledgeflow/module/ai/service/RagRetriever.java
assert_code "$RAG" 'candidate-size:20'  "02: 召回候选数仍为 20"
assert_code "$RAG" 'top-k:5'            "02: 精排 topK 仍为 5"
assert_code "$RAG" 'score-threshold:0.5' "02: 相似度下限仍为 0.5"
assert_code "$RAG" 'content_chunks'     "02: 集合名仍为 content_chunks"
assert_code "$RAG" 'RagContext.empty(query)' "02: 检索失败降级路径仍存在"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/Reranker.java \
            'naturalOrder' "02: Reranker 降级静态方法仍存在"
```

```bash
assert_doc "docs/tech/02-召回精排两段式检索.md"
```

- [ ] **Step 4: 运行核验**

```bash
./scripts/verify-docs.sh
```

预期：累计 14 项 ok，失败 0 项。

- [ ] **Step 5: 提交**

```bash
git add docs/tech/02-召回精排两段式检索.md scripts/verify-docs.sh
git commit -m "docs: add teaching doc on two-stage retrieve-then-rerank"
```

---

### Task 3: 03-自建重排服务与降级设计

**Files:**
- Create: `docs/tech/03-自建重排服务与降级设计.md`
- Modify: `scripts/verify-docs.sh`
- Read-only 素材: `CrossEncoderReranker.java`（183 行）、`LlmReranker.java`（163 行）、`NoOpReranker.java`（26 行）、`reranker-service/app.py`、`reranker-service/README.md`

**Interfaces:**
- Consumes: Task 2 的结论「重排是可替换策略，失败必须退回向量顺序」。本篇不再重复论证为什么要重排。
- Produces: 无下游依赖。

**已核实的事实:**

| 事实 | 位置 |
| :--- | :--- |
| 三个实现用 `@ConditionalOnProperty(name="app.rag.rerank.provider")` 互斥装配 | 三个类的类注解 |
| `cross-encoder` 是默认值（`matchIfMissing = true`） | `CrossEncoderReranker` 类注解 |
| `llm` 对应 `LlmReranker`，`none` 对应 `NoOpReranker` | 各类注解 |
| 重排超时默认 2000ms，连接超时 1s | `CrossEncoderReranker` 字段 + `HttpClient` 构建 |
| 注释明确：超时必须短，重排在用户请求关键路径上 | `timeoutMs` 字段注释 |
| 请求体同时写 `documents` 和 `texts` 两个字段以兼容托管 API 与 TEI | `buildRequestBody()` |
| 响应解析兼容 `{"results":[...]}` 与顶层数组两种形状 | `parseIndices()` |
| `parseIndices` 做越界过滤 + `LinkedHashSet` 去重 | 同上 |
| 候选数 ≤ topK 时跳过网络往返 | `rerank()` 开头 |
| 所有异常路径均返回 `Reranker.naturalOrder(topK)` | `rerank()` catch 块 |
| `LlmReranker` 用列表式（listwise）：所有候选一次 prompt，非逐条打分 | 类注释 |
| `LlmReranker` 正则字符类含负号，防模型返回负数下标导致整体匹配失败 | `INDEX_ARRAY` 注释 |
| `NoOpReranker` 的存在意义是评测对照组 | 类注释 |
| Python 服务模型 `BAAI/bge-reranker-v2-m3`，`MAX_LENGTH` 默认 1024，`BATCH_SIZE` 默认 8，端口 8081 | `app.py` 常量 |
| 选原生进程而非 Docker：macOS 上 Docker 不能直通 GPU，原生进程可走 PyTorch MPS | `app.py` 模块注释 |
| fp16 在 MPS 上原生支持，权重约 2.3GB → 1.1GB；CPU 上 fp16 无收益故退回 fp32 | `app.py` 模块注释 |
| 推理用 `@torch.inference_mode()`，分批避免过大注意力中间张量 | `score_pairs()` |
| fp16 结果转回 float32 再序列化 | `score_pairs()` |

- [ ] **Step 1: 核对素材**

```bash
sed -n '38,70p'   src/main/java/com/ray/knowledgeflow/module/ai/service/CrossEncoderReranker.java
sed -n '115,175p' src/main/java/com/ray/knowledgeflow/module/ai/service/CrossEncoderReranker.java
sed -n '1,50p'    reranker-service/app.py
sed -n '105,160p' reranker-service/app.py
grep -rn "app.rag.rerank" src/main/resources/ || echo "配置未落到 application.yml，全部走默认值"
```

最后一条 grep 的结果必须写进文档——如果配置项没有出现在 `application.yml` 里，说明线上实际跑的是各字段的 `@Value` 默认值，这一点必须讲清楚。

- [ ] **Step 2: 写文档**

**① 这个问题为什么难** — 场景：重排要接入模型，摆在面前三条路（托管 API、自部署、用现有 LLM），三者在延迟、成本、可用性上的取舍完全不同；更棘手的是**重排处于用户请求的关键路径上**——它不像索引那样可以慢慢跑，用户在等着看答案。一个 5 秒才返回的重排服务，比不做重排更糟。

由此引出本篇的两条主线：**选型**（怎么选）与**降级**（选错了/挂了怎么办）。

**② 原理** — 两部分：

*第一部分：三种重排路线的结构差异*

| 路线 | 模型形态 | 调用次数 | 典型延迟量级 | 依赖 |
| :--- | :--- | :--- | :--- | :--- |
| 交叉编码器（pointwise） | 专用小模型（bge-reranker-v2-m3 约 5.7 亿参数） | 每候选 1 次前向，可批量 | 低 | 需额外部署推理服务 |
| 大模型列表式（listwise） | 通用 LLM | 全部候选 1 次调用 | 高 | 复用已有 LLM，零额外部署 |
| 不重排 | — | 0 | 0 | — |

**不写具体毫秒数**，除非实测。用「低/高」和「专用模型比通用 LLM 小一到两个数量级」这类可辩护的相对表述。

*第二部分：为什么列表式而非逐条打分*

让 LLM 逐条给候选打分要 N 次调用，延迟和成本都不可接受；列表式把所有候选编号后放进同一个 prompt，让模型一次返回排好序的下标数组。代价是模型可能返回不合法输出（多余文字、越界下标、重复下标），因此**解析必须做防御**。

*第三部分：降级为什么是设计而非补丁*

重排是**增强**不是**必需**——没有它，系统退回向量顺序仍能工作。因此所有失败路径都应收敛到「退回向量原始顺序」，而不是抛异常。这个约定写在 `Reranker` 接口注释里，并由接口静态方法 `naturalOrder()` 提供统一实现。

**③ 代码走读**：

- 三个类的 `@ConditionalOnProperty` 注解（各 1 行，并列展示）：讲清 `matchIfMissing = true` 让交叉编码器成为默认，以及三者互斥、容器里只会有一个 `Reranker` Bean。这是 Spring 条件装配做策略切换的标准手法，比自己写 `if/else` 工厂干净。
- `CrossEncoderReranker.rerank()` 的短路（约 4 行）：`candidates.size() <= topK` 直接返回，省一次网络往返。
- `buildRequestBody()`（约 12 行）：同时写 `documents` 和 `texts`。讲清动机——一份请求体同时满足 Cohere 风格托管 API 和 HuggingFace TEI，切换服务只改 URL 不改代码。这是**用冗余换可移植性**的典型取舍。
- `parseIndices()`（约 25 行）：本篇最该细讲的一段。三重防御——(a) 兼容顶层数组与 `results` 字段两种形状；(b) 越界下标直接丢弃，因为它们会导致 `candidates.get(index)` 抛异常；(c) `LinkedHashSet` 去重且保序。强调一句话：**外部服务的返回值不可信**，哪怕是自己部署的服务。
- 四条降级路径（约 15 行）：非 2xx、解析为空、`InterruptedException`（注意恢复中断标志）、其他异常——全部 `return Reranker.naturalOrder(topK)`。
- `LlmReranker` 的 `INDEX_ARRAY` 正则（约 3 行）：字符类里那个负号。讲清为什么——模型偶尔返回 `[-1, 2, 3]`，若正则不容忍负号则整体匹配失败，本可用的 2、3 也一起丢了。**这是一个只有踩过才会写的细节，值得单独讲。**
- `app.py` 的 `pick_device()` 与 fp16 选择（约 10 行）：MPS 上 fp16 权重减半、CPU 上 fp16 无收益故退回 fp32。
- `app.py` 的 `score_pairs()`（约 18 行）：`@torch.inference_mode()`、分批、`logits.float().cpu()`。讲清为什么要 `.float()`——fp16 直接序列化成 JSON 有精度问题。

**④ 为什么不用 X 而用 Y**：

| 方案 | 取舍 |
| :--- | :--- |
| 托管重排 API（Cohere / Jina / 硅基流动） | 零运维，但有网络往返、按量计费、且笔记内容要出网。**本项目的请求体格式就是对齐它们的，随时可切** |
| Docker 部署重排服务 | 环境一致性好，但 macOS 上 Docker 无法直通 GPU，容器内只能跑 CPU，还多一层虚拟机内存开销。这是**开发机是 Apple Silicon** 这个具体约束导致的决策，换 Linux + NVIDIA 服务器结论会反转 |
| 原生进程 + MPS（本项目） | 吃到 Apple Silicon GPU，省掉虚拟机开销；代价是部署脚本与环境隔离要自己管 |
| 直接用 LLM 列表式重排 | 零额外部署，但延迟高、成本按 token 计。**本项目保留了这个实现**（`provider=llm`），作为不想部署 Python 服务时的备选 |

强调：Docker 那条决策的理由是**平台特定**的，面试时要主动说明适用边界，不要说成「Docker 不适合部署推理服务」这种以偏概全的结论。

**⑤ 追问预演**：

- 第一层：「为什么重排超时设 2 秒？」
  → 重排在用户请求关键路径上，宁可退回向量顺序也不让用户干等。2 秒是「用户可忍受的额外等待」的经验取值，不是实测得出。**注意：这里要老实说是拍的。**
- 第二层：「超时退回向量顺序，用户看到的是降级结果，但他不知道。你怎么发现线上重排一直在超时？」
  → 当前只有 `log.warn`，**没有指标、没有告警**。这是真实缺陷。降级设计做对了一半——失败不阻断做到了，失败可观测没做到。改进方向：给降级路径打计数器（Micrometer），超过阈值告警。
- 第三层：「你的重排服务是单进程 uvicorn，模型在 lifespan 里加载。那服务重启期间，Java 侧发生什么？还有，`/health` 返回 `loading` 状态，Java 侧读了吗？」
  → 重启期间连接被拒 → 走 catch → 退回向量顺序，**功能正确**。但 `/health` 的 `loading` 状态 Java 侧**完全没有消费**，没有健康检查、没有熔断——服务挂掉后每次请求都要白等一个连接超时（1 秒）。改进：接 Resilience4j 熔断器，连续失败后快速失败，不再尝试连接。**项目里已经用了 Resilience4j 做 AI 限流**（`AiProcessConsumer`），熔断是现成能力没用上。

**⑥ 现有实现的坑与改进方向**：

1. **降级不可观测**：只打日志，无指标无告警（见 ⑤ 第二层）。
2. **无熔断**：重排服务挂掉后每次请求白等连接超时；Resilience4j 已在项目中使用但未用于此处。
3. **`/health` 端点无人消费**。
4. **请求体同时发 `documents` 和 `texts`**：候选文本在网络上传了两份，候选多时请求体翻倍。可移植性的代价，应按 provider 只发其一。
5. **重排服务单点单进程**，无副本、无排队控制；`BATCH_SIZE=8` 固定，并发请求会互相排队。
6. **配置项落地情况**：写明 Step 1 中 grep 的实际结果——若 `app.rag.rerank.*` 未出现在 `application.yml`，说明全部走 `@Value` 默认值，那么「可配置」目前只是代码层面的能力，运维层面尚未暴露。

- [ ] **Step 3: 追加断言**

```bash
CE=src/main/java/com/ray/knowledgeflow/module/ai/service/CrossEncoderReranker.java
assert_code "$CE" 'havingValue = "cross-encoder", matchIfMissing = true' "03: 交叉编码器仍为默认 provider"
assert_code "$CE" 'timeout-ms:2000' "03: 重排超时仍为 2000ms"
assert_code "$CE" 'root.putArray("texts")' "03: 请求体仍双写 texts 字段"
assert_code "$CE" 'LinkedHashSet' "03: 解析仍用 LinkedHashSet 去重保序"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/LlmReranker.java \
            '[-\d,\s]*' "03: LLM 重排正则仍容忍负号"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/NoOpReranker.java \
            'havingValue = "none"' "03: NoOp 重排器仍绑定 provider=none"
assert_code reranker-service/app.py 'BAAI/bge-reranker-v2-m3' "03: 重排模型未变"
assert_code reranker-service/app.py 'torch.inference_mode' "03: 推理仍用 inference_mode"
```

```bash
assert_doc "docs/tech/03-自建重排服务与降级设计.md"
```

- [ ] **Step 4: 运行核验**

```bash
./scripts/verify-docs.sh
```

预期：累计 23 项 ok，失败 0 项。

- [ ] **Step 5: 提交**

```bash
git add docs/tech/03-自建重排服务与降级设计.md scripts/verify-docs.sh
git commit -m "docs: add teaching doc on self-hosted reranker and degradation design"
```

---

### Task 4: 04-混合检索与 RRF 融合

**Files:**
- Create: `docs/tech/04-混合检索与RRF融合.md`
- Modify: `scripts/verify-docs.sh`
- Read-only 素材: `SearchServiceImpl.java`（674 行；重点 366、380-500、511、580-615）

**Interfaces:**
- Consumes: 01-03 篇建立的向量检索背景。
- Produces: 无下游依赖。

**已核实的事实:**

| 事实 | 位置 |
| :--- | :--- |
| `searchContentHybrid` 中 `final int k = 60`，候选数 `Math.max(size * 2, 60)` | `SearchServiceImpl.java:366` 附近 |
| `searchSimilarNoteIds` 中 `final int k = 60`，候选数 `Math.max(limit * 3, 50)` | `:511` 附近 |
| **k=60 在两处各硬编码一次** | 上述两处 |
| ES 与 Qdrant 用 `CompletableFuture.supplyAsync` 并行，`allOf(...).join()` 汇合 | 两个方法内 |
| 融合公式 `rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum)`，`i` 从 0 起故 `+1` | 两处 |
| ES 多字段权重 `title^3`、`summary^2`、`content`、`tags` | 两个方法的 `multiMatch` |
| ES 用 `SourceFilter` 只取 `id` 字段 | 两个方法 |
| 推荐场景（`searchSimilarNoteIds`）过滤 `isPublic=1`，且**移除了 userId 过滤**以便跨用户推荐 | `:520` 附近，含注释 `Removed userId filter to recommend from whole community` |
| 混合搜索场景保留 `userId` 过滤，且取详情时再按 userId 收口一次（双重保险） | `searchContentHybrid` 的 mapper 查询 |
| ES 分支 catch 后返回空列表（静默降级） | 两个方法 |
| `searchContentHybrid` 的 total = `sortedIds.size()`（融合候选数，受 candidateSize 封顶） | `:466` 附近 |
| `searchContentSemantic` 的 total = `resultList.size()`（仅当页），注释承认 Qdrant 不易取 total | `:353` 附近 |
| 向量检索关闭 payload（`setEnable(false)`），只要 ID | `searchContentHybrid` |
| `syncAll` 先 `deleteAll()` 再逐条 `save()` | 文件末尾 |

- [ ] **Step 1: 核对素材行号**

```bash
sed -n '360,372p' src/main/java/com/ray/knowledgeflow/module/search/service/impl/SearchServiceImpl.java
sed -n '443,470p' src/main/java/com/ray/knowledgeflow/module/search/service/impl/SearchServiceImpl.java
sed -n '505,530p' src/main/java/com/ray/knowledgeflow/module/search/service/impl/SearchServiceImpl.java
sed -n '592,612p' src/main/java/com/ray/knowledgeflow/module/search/service/impl/SearchServiceImpl.java
sed -n '348,356p' src/main/java/com/ray/knowledgeflow/module/search/service/impl/SearchServiceImpl.java
```

- [ ] **Step 2: 写文档**

**① 这个问题为什么难** — 用两个互补的失败场景，说明单一检索路各自的盲区：

> 场景 A：搜「ConcurrentModificationException」。这是个精确的类名，BM25 一击即中。向量检索会返回一堆「并发」「异常处理」的笔记——语义相近，但用户要的就是那一个类名。
>
> 场景 B：搜「怎么让搜索既准又能理解意思」。用户笔记里那篇写的是「混合检索与 RRF 融合」，一个词都对不上。BM25 返回空，向量检索一击即中。

结论：两条路互补。但**合并结果时立刻撞上一个硬问题**——BM25 分数是无上界的正实数（依赖词频、文档长度、语料统计），余弦相似度在 [−1, 1]。这两个数**不可直接相加，也不能简单归一化后相加**：归一化需要知道分数分布，而 BM25 的分布随查询词、随语料变化，没有稳定的量纲基准。

**② 原理** — RRF 的推导，本篇核心：

1. **核心思路：扔掉分数，只用排名。** 排名是无量纲的序数——「ES 认为它排第 3」和「向量认为它排第 3」是可比的，而「BM25 分数 7.2」和「余弦 0.83」不可比。这一步把跨系统分数融合问题转化成了跨系统**排名**融合问题，绕开了量纲。
2. **为什么用倒数。** 需要一个随排名递减的函数。线性递减（如 `N - rank`）的问题是它对头部和尾部一视同仁——第 1 名和第 2 名的差距，等于第 50 名和第 51 名的差距。而检索里头部差距远比尾部重要。倒数 `1/rank` 天然满足：1 与 1/2 差 0.5，1/50 与 1/51 只差 0.0004。
3. **k 是干什么的。** 完整公式 `score(d) = Σ_i 1/(k + rank_i(d))`。若没有 k，第 1 名得 1 分、第 2 名 0.5 分——**一个系统的第一名，抵得过另一个系统的第 2、3 名之和**，单路强信号过于霸道。k 是个平滑项，把曲线压平：k=60 时第 1 名得 1/61 ≈ 0.0164，第 2 名 1/62 ≈ 0.0161，差距被大幅压缩，于是**「两路都排前列」比「一路排第一」更有优势**——这正是融合想要的行为。
4. **k 的两个极限。** k→0 时退化为纯 `1/rank`，头部极度霸道；k→∞ 时所有排名的分数趋于相等，退化为「按被几路召回的次数投票」。60 这个值来自 Cormack、Clarke、Buettcher 2009 年提出 RRF 的论文中的实验设置，**在信息检索领域被广泛沿用为默认值**。要老实说明：本项目直接采用了这个约定俗成的默认值，没有在自己的数据上调过。
5. 举一个三行的具体算例：文档 X 在 ES 排 1、向量排 50；文档 Y 在两路都排 5。算出 X = 1/61 + 1/110 ≈ 0.0255，Y = 1/65 + 1/65 ≈ 0.0308，Y 胜出。用这个算例把「两路一致的中等排名 > 一路突出」这个性质讲实。

**③ 代码走读**（用 Step 1 的真实行号）：

- 两路并行（约 10 行）：`CompletableFuture.supplyAsync` × 2 + `allOf(...).join()`。讲清为什么必须并行——串行的话总延迟是两者之和，而两路无依赖关系。
- 融合循环（约 8 行）：`rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum)`。**重点讲 `+1`**：`i` 是从 0 起的下标，而 RRF 公式里的 rank 从 1 起，漏掉 `+1` 会让第一名的分母变成 k 而非 k+1。同时讲 `merge` + `Double::sum` 如何优雅处理「两路都召回了同一个 ID」的情况。
- ES 查询的字段权重（约 6 行）：`title^3`、`summary^2`。说明这是**路内**的权重，与 RRF 的**跨路**融合是两个层次——先在 ES 内部把标题匹配抬高，再拿最终排名去参与融合。
- `SourceFilter` 只取 `id`（约 8 行）：融合阶段只需要 ID，不取 `_source` 能显著减少 ES 的网络传输。向量侧对应的是 `setWithPayload(false)`，同一思路。
- 两个方法的**过滤策略差异**（约 6 行）：`searchContentHybrid` 按 `userId` 过滤（私人搜索），`searchSimilarNoteIds` 按 `isPublic=1` 过滤并**刻意移除** userId（社区推荐要跨用户）。指出源码里那行注释 `Removed userId filter to recommend from whole community`——同一个融合逻辑服务两个权限模型完全不同的场景。
- 详情查询的双重收口（约 6 行）：`searchContentHybrid` 取详情时 mapper 里又带了一次 `eq(userId)`。讲清为什么要防御两次——检索侧的过滤依赖索引数据的正确性，而索引是异步同步的、可能滞后或有脏数据；最终返回给用户的数据以 MySQL 为准再卡一道。

**④ 为什么不用 X 而用 Y**：

| 方案 | 为什么不用 |
| :--- | :--- |
| 分数加权求和 `α·BM25 + β·cosine` | 量纲不可比（① 的核心问题）。要用必须先归一化，而 BM25 无稳定上界，归一化基准随查询漂移 |
| Min-Max 归一化后加权 | 归一化范围由**当次结果集**决定，同一文档在不同查询下的归一化分数不可比；且结果集里只有一条时归一化退化 |
| ES 8.x 原生 RRF / `knn` 混合检索 | ES 8.x 确有原生 RRF，能省掉应用层融合。**这是本项目当时没有采用的方案，属于事后才了解的选项**——必须标注为事后分析，不要说成「我评估后放弃了」。代价是把向量能力绑到 ES 上，而本项目的向量在 Qdrant |
| 只用其中一路 | ① 的两个场景各证明了一路不够 |

**⑤ 追问预演**：

- 第一层：「RRF 的 k 为什么取 60？」
  → 讲平滑作用与两个极限。**明确说明是沿用 Cormack 等 2009 年论文的默认值，没有在本项目数据上调过。**
- 第二层：「改成 10 会怎样？」
  → 头部权重变陡，单路的第一名话语权变大，融合更接近「取两路各自的头部」；改大则趋向「按召回次数投票」。方向清楚，但**具体在本项目数据上哪个更好，没测过**——因为没有标注评测集。这个「没有评测集」是本项目 RAG/检索部分的共性短板，值得主动提。
- 第三层：「你说 RRF 靠排名规避了量纲问题。但有个前提——两路的排名要有可比的质量。如果 ES 那路根本没匹配上，返回了 60 条勉强沾边的结果，它们照样会拿到 1/61、1/62 的分数去污染融合结果。你怎么处理？」
  → **这是本篇最硬的一问，当前实现没有处理。** RRF 只看排名不看分数，代价就是它**丢掉了「这一路到底匹配得好不好」的绝对信息**。缓解手段：给每路设最低分数门槛，低于门槛的不进入融合；或按每路的整体质量给动态权重。当前代码两条都没做。这是 RRF 的固有权衡——用量纲无关换绝对信息缺失，不是实现疏漏。
- 追加一问（若被问到分页）：「你这个混合搜索的 total 是准的吗？」→ 见 ⑥。

**⑥ 现有实现的坑与改进方向**：

1. **`k = 60` 在 `:366` 与 `:511` 各硬编码一次**，改一处会漏另一处。应提为配置常量。
2. **两处分页 total 都不准，成因不同——不可混为一谈。**
   - `searchContentHybrid`：`total = sortedIds.size()`，即融合候选数，被 `candidateSize`（`max(size*2, 60)`）封顶。语料里有 500 条命中，前端也只会看到最多 120 左右。
   - `searchContentSemantic`：`total = resultList.size()`，即当前页条数，源码注释已承认 Qdrant 不易取精确 total。
3. **ES 分支静默降级**：catch 后返回空列表，ES 故障时混合检索无声退化为纯向量检索，调用方与用户都无从感知。与 03 篇「降级不可观测」是同一类问题。
4. **无低分门槛**（见 ⑤ 第三层），弱匹配结果会污染融合。
5. **`syncAll` 先 `deleteAll()` 再逐条 `save()`**：全量同步期间索引处于空/半空状态，线上搜索会短暂返回空结果；且未用 ES 批量 API，逐条 `save()` 在数据量大时很慢。改进：别名切换（写新索引 → 原子切别名 → 删旧索引）。
6. **两个方法的融合逻辑几乎逐行重复**，只有过滤条件和返回形状不同，应抽公共方法。

- [ ] **Step 3: 追加断言**

```bash
SS=src/main/java/com/ray/knowledgeflow/module/search/service/impl/SearchServiceImpl.java
assert_code "$SS" 'final int k = 60; // RRF constant' "04: searchSimilarNoteIds 的 k 仍为 60"
assert_code "$SS" '1.0 / (k + i + 1)' "04: RRF 融合公式未变"
assert_code "$SS" 'title^3' "04: ES 标题权重仍为 3"
assert_code "$SS" 'summary^2' "04: ES 摘要权重仍为 2"
assert_code "$SS" 'Removed userId filter to recommend from whole community' "04: 推荐场景仍跨用户召回"
assert_code "$SS" 'int total = sortedIds.size();' "04: 混合搜索 total 仍取融合候选数"
assert_code "$SS" 'noteRepository.deleteAll();' "04: syncAll 仍先清空索引"
```

**注意**：`assert_code` 用 `grep -F` 做字面匹配，上面第一条含 `//` 注释文本，若代码注释被改动会 FAIL——这正是期望行为（提示文档需复核）。

```bash
assert_doc "docs/tech/04-混合检索与RRF融合.md"
```

- [ ] **Step 4: 运行核验**

```bash
./scripts/verify-docs.sh
```

预期：累计 31 项 ok，失败 0 项。

- [ ] **Step 5: 提交**

```bash
git add docs/tech/04-混合检索与RRF融合.md scripts/verify-docs.sh
git commit -m "docs: add teaching doc on hybrid search and RRF fusion"
```

---

### Task 5: 05-异步链路与最终一致性

**Files:**
- Create: `docs/tech/05-异步链路与最终一致性.md`
- Modify: `scripts/verify-docs.sh`
- Read-only 素材: `RabbitMQConfig.java`、`AiProcessConsumer.java`、`EngagementSyncConsumer.java`、`GraphSyncListener.java`、`NoteServiceImpl.java`（:82、:110、:114-116、:127、:167、:184、:204）

**Interfaces:**
- Consumes: 无。
- Produces: 「事务提交前发布」这一缺陷的完整分析，Task 7（总览）在数据流图中引用本篇结论。

**已核实的事实:**

| 事实 | 位置 |
| :--- | :--- |
| 5 个队列：`content.fetch`、`ai.process`、`index.update`、`learning.reminder`、`engagement.sync` | `RabbitMQConfig` 常量 |
| 5 个 DirectExchange，一一对应 | 同上 |
| **只有 `engagement.sync.queue` 配了 DLX**（`engagement.dlx` + `engagement.dlq` 路由键） | `engagementSyncQueue()` |
| 其余 4 个队列都是裸 `QueueBuilder.durable(...).build()` | 同上文件 |
| `EngagementSyncConsumer` 异常向上抛出以触发重试，耗尽后入死信 | 方法注释 + 无 catch |
| `EngagementSyncConsumer` 幂等设计：消息只带 `noteId`，以数据库为准重算点赞数覆盖 Redis，重复/乱序消费收敛到同一结果，无需幂等表 | 类注释 |
| `AiProcessConsumer` catch 所有异常仅打日志，源码含 `// TODO: 可以考虑重试机制或发送到死信队列` | `handleAiProcessTask()` |
| `AiProcessConsumer` 用 `@RateLimiter(name = "ai-service")`（Resilience4j）限流 | 方法注解 |
| 项目并存两套异步机制：Spring `@EventListener`（7 个 Listener 中的 3 个）与 RabbitMQ `@RabbitListener` | `find` 结果 |
| `GraphSyncListener` 用 `@Async @EventListener`，非 `@TransactionalEventListener` | 类内两个方法 |
| `NoteServiceImpl.createNote` 标注 `@Transactional(rollbackFor = Exception.class)`（:82） | `:82` |
| 该方法**在事务内**于 `:110` 发布 `NoteSavedEvent`、`:114-116` 投递三条 MQ 任务 | `:110`、`:114-116` |
| `updateNote`(:127)、`deleteNote`(:184) 同样带事务并在事务内发布事件（:167、:204） | 对应行 |

- [ ] **Step 1: 核对素材行号**

```bash
sed -n '112,130p' src/main/java/com/ray/knowledgeflow/config/RabbitMQConfig.java
sed -n '92,112p'  src/main/java/com/ray/knowledgeflow/config/RabbitMQConfig.java
sed -n '30,62p'   src/main/java/com/ray/knowledgeflow/module/ai/message/AiProcessConsumer.java
sed -n '80,120p'  src/main/java/com/ray/knowledgeflow/module/note/service/impl/NoteServiceImpl.java
grep -n "@Transactional" src/main/java/com/ray/knowledgeflow/module/note/service/impl/NoteServiceImpl.java
grep -rn "TransactionalEventListener" src/main/java || echo "确认：项目中未使用 @TransactionalEventListener"
grep -rn "LEARNING_REMINDER_QUEUE" src/main/java || echo "确认：提醒队列无消费者"
grep -rn "CONTENT_FETCH_QUEUE\|INDEX_UPDATE_QUEUE" src/main/java | grep -i "RabbitListener\|queues ="
```

后三条 grep 的结果直接决定 ⑥ 段怎么写，必须实际执行：

- 第 2 条决定 ⑥.1（若已引入 `@TransactionalEventListener`，说明竞态已修，⑥.1 须改写为「曾存在的问题及修法」）。
- 第 3 条决定 ⑥.6：若 `LEARNING_REMINDER_QUEUE` 只在 `RabbitMQConfig` 中出现、无任何 `@RabbitListener` 引用，则该队列目前是**声明了但无人消费**的预留队列，文档须如实说明，不得暗示它在工作。
- 第 4 条确认 `content.fetch` 与 `index.update` 两个队列的消费者是否存在，用于核实「5 队列」的实际启用情况。

- [ ] **Step 2: 写文档**

**① 这个问题为什么难** — 场景：

> 用户点「保存笔记」。这一下要触发：写 MySQL、同步 ES 索引、切片嵌入进 Qdrant、解析 `[[链接]]` 写 Neo4j、调 LLM 生成摘要、调 LLM 抽标签。后四件事每件都要几百毫秒到几秒。如果同步做完再返回，用户点保存要转圈十几秒。

所以必须异步。但异步立刻带来三个新问题，本篇分别回答：

1. 主库写成功、异步任务失败了怎么办？（可靠性）
2. 消息重复投递或乱序到达怎么办？（幂等）
3. 异步任务跑起来时，主库的事务提交了吗？（时序）

第三个问题最隐蔽，也是本项目**确实踩了**的那个。

**② 原理** — 三部分：

*第一部分：两套异步机制的分工*

项目里并存 Spring 应用事件（`@EventListener`）和 RabbitMQ（`@RabbitListener`）。讲清本质差异：

| | Spring 事件 | RabbitMQ |
| :--- | :--- | :--- |
| 边界 | 进程内 | 跨进程 |
| 持久化 | 无，进程重启即丢 | 队列持久化，重启不丢 |
| 重试 | 无内建机制 | 有，可配死信 |
| 成本 | 一次方法调用 | 序列化 + 网络 + broker |

选择标准：**丢了要不要紧**。丢了可以靠全量重建补回来的（ES 索引、图谱同步）走事件；丢了就真丢了或代价高的（AI 生成、计数同步）走 MQ。

*第二部分：可靠性阶梯*

从弱到强：无重试 → 自动重试 → 重试 + 死信队列。讲清死信队列的作用：它不是「自动修复」，而是**把失败消息保留下来**，避免重试耗尽后被静默丢弃，人工或补偿任务后续可以捞出来处理。

**本项目在这个阶梯上是不齐的**——只有 `engagement.sync` 到了最高档，其余 4 个队列停在最低档。这是本篇的核心对照。

*第三部分：幂等的两条路*

- **幂等表**：记消息 ID，消费前查重。通用但要多一次 IO 和一张表。
- **让操作天然幂等**：设计成重复执行结果相同。`EngagementSyncConsumer` 走的是这条——消息只带 `noteId`，消费动作是「以数据库为准重算点赞数并覆盖 Redis」。这是**覆盖式**而非**增量式**：若消息带的是「+1」，重复消费就会多加；带 `noteId` 让消费方自己去算，重复消费、乱序到达都收敛到同一结果。

这一点值得展开讲——它是「把幂等性设计进消息格式」的典型例子，比事后加幂等表更根本。

*第四部分：事务与发布的时序*

`@Transactional` 方法内 `publishEvent` / 发 MQ 消息时，事务**尚未提交**。若监听方是同步的（默认 `@EventListener`），它在同一线程同一事务里跑，看得到未提交数据，问题不大；但一旦加上 `@Async`，监听器换到另一个线程，那个线程**看不到未提交的事务**——它去查这条笔记可能查不到。MQ 更糟：消息一投递就可能被别的进程消费，而事务可能几百毫秒后才提交，甚至回滚（此时消息已经发出去了，指向一条根本不存在的笔记）。

正确做法是 `@TransactionalEventListener(phase = AFTER_COMMIT)`，让监听器在事务提交后才触发。MQ 侧则应在 `AFTER_COMMIT` 回调里投递。

**③ 代码走读**（用 Step 1 的真实行号）：

- `engagementSyncQueue()`（约 8 行）与紧邻的 `learningReminderQueue()`（约 4 行）**并列展示**——一个配了 `deadLetterExchange`，一个是裸 durable 队列。把可靠性差异直接呈现在两段相邻代码上，比文字描述有力。
- `EngagementSyncConsumer.handleEngagementSync()`（约 12 行）：**没有 try-catch**。讲清这是有意为之——异常向上抛出才能触发 Spring AMQP 的重试，catch 住等于放弃了重试和死信。同时讲类注释里的幂等设计。
- `AiProcessConsumer.handleAiProcessTask()` 的 catch 块（约 6 行）：贴出那行 `// TODO: 可以考虑重试机制或发送到死信队列`。**这是同一个项目里两种可靠性等级的直接对照**，也是本篇最有说服力的素材——不回避，直接摆出来分析。
- `@RateLimiter(name = "ai-service")`（1 行）：讲清为什么 AI 队列要限流——LLM 有 QPS 限制且按量计费，MQ 天然会把积压的消息一次性推给消费者，不限流会瞬间打爆配额。这是**MQ 削峰的下半场**：MQ 削了入口的峰，限流削了出口的峰。
- `NoteServiceImpl.createNote`（约 15 行）：把 `@Transactional`(:82)、`publishEvent`(:110)、三条 MQ 投递(:114-116) 放在一起看，指出它们同处一个事务边界内。
- `GraphSyncListener.handleNoteSaved()`（约 12 行）：`@Async @EventListener` 两个注解并置，然后 `noteService.getNoteDetail(...)` 回查数据库——**竞态就在这里**：异步线程回查一条可能尚未提交的笔记。

**④ 为什么不用 X 而用 Y**：

| 方案 | 取舍 |
| :--- | :--- |
| 全部同步执行 | 用户等十几秒，不可接受 |
| 全部走 MQ | 一致，但 ES/图谱同步这类可重建的任务也要付序列化 + broker 成本；且本地开发要起 RabbitMQ 才能跑通 |
| 全部走 Spring 事件 | 轻，但进程重启丢消息、无重试。AI 摘要跑一半重启就永久丢失 |
| 分场景选择（本项目） | 按「丢了要不要紧」分流。**但代价是认知负担**——维护者要记住哪个走事件、哪个走 MQ，且两套的可观测性、重试语义完全不同 |
| 事务性发件箱（Transactional Outbox） | 把消息先写进同库的 outbox 表（与业务同事务），再由轮询器投递。能彻底解决 ② 第四部分的时序问题，代价是多一张表和一个轮询器。**本项目未采用，属改进方向** |

**⑤ 追问预演**：

- 第一层：「为什么有的走 Spring 事件，有的走 MQ？」
  → 讲「丢了要不要紧」的分流标准，举 ES 索引（可 `syncAll` 重建）vs AI 摘要（丢了要重新花钱调 LLM）的对比。
- 第二层：「你的 AI 队列，消费失败了会怎么样？」
  → **直接承认：catch 住只打日志，消息 ack 掉了，任务永久丢失，源码里还留着 TODO。** 而 engagement 队列做对了——不 catch、抛出去触发重试、耗尽进死信。同一个项目里两种做法并存，说明当时是逐步演进的，engagement 是后来才补上的。这样讲比硬说「都设计好了」可信得多。
- 第三层：「你在 `@Transactional` 方法里发布事件、发 MQ 消息。而 `GraphSyncListener` 是 `@Async @EventListener`。异步线程去查这条笔记的时候，事务提交了吗？」
  → **不一定。** 完整讲清这个竞态：`publishEvent` 在 :110、事务在方法返回后才提交，`@Async` 监听器立刻在另一线程执行 `getNoteDetail()`，可能查不到刚插入的行。实际表现是图谱同步偶发失败并打 error 日志——而且因为 catch 住了，**表现为静默的图谱缺失**，不是显式报错。更严重的是 MQ：事务若回滚，消息已经发出去了，消费者会拿到一个不存在的 noteId。
  修法：`@TransactionalEventListener(phase = AFTER_COMMIT)`，MQ 投递同理挪到提交后。彻底方案是事务性发件箱。
  **这一问要主动讲透**——能自己指出这个竞态，比藏着被问出来强得多。

**⑥ 现有实现的坑与改进方向**：

1. **事务提交前发布事件与消息**（⑤ 第三层）。`GraphSyncListener` 的 `@Async @EventListener` 与 `NoteServiceImpl:110/114-116` 构成竞态。改用 `@TransactionalEventListener(AFTER_COMMIT)`。Step 1 已确认项目中未使用该注解。
2. **可靠性等级不齐**：5 个队列只有 `engagement.sync` 有死信，其余 4 个失败即丢。
3. **`AiProcessConsumer` catch 吞异常**，源码 TODO 未处理。
4. **异步失败静默**：`GraphSyncListener` 两个方法都 catch 后只打 error 日志，用户侧无任何感知，图谱缺一个节点没人会发现。
5. **无消息追踪**：一次保存扇出 1 个事件 + 3 条 MQ 消息，没有关联 ID（traceId），出问题无法把它们串起来排查。
6. **`learning.reminder` 队列**：Step 1 中确认它是否真有消费者；若无，应在文档中说明它目前是预留的。

- [ ] **Step 3: 追加断言**

```bash
MQC=src/main/java/com/ray/knowledgeflow/config/RabbitMQConfig.java
assert_code "$MQC" '.deadLetterExchange(ENGAGEMENT_DLX)' "05: engagement 队列仍配置死信"
assert_code "$MQC" 'QueueBuilder.durable(LEARNING_REMINDER_QUEUE).build()' "05: 提醒队列仍无死信"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/message/AiProcessConsumer.java \
            'TODO: 可以考虑重试机制或发送到死信队列' "05: AI 消费者的 TODO 仍在"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/message/AiProcessConsumer.java \
            '@RateLimiter(name = "ai-service")' "05: AI 消费者仍有限流注解"
assert_code src/main/java/com/ray/knowledgeflow/module/graph/listener/GraphSyncListener.java \
            '@EventListener' "05: 图谱监听器仍用普通 EventListener"
assert_code src/main/java/com/ray/knowledgeflow/module/note/service/impl/NoteServiceImpl.java \
            '@Transactional(rollbackFor = Exception.class)' "05: createNote 仍带事务"
```

追加一条**反向断言**（确认竞态尚未被修复；若已修复，文档 ⑥ 第 1 条需改写）：

```bash
if grep -rqF 'TransactionalEventListener' src/main/java; then
  echo "  FAIL  05: 已引入 @TransactionalEventListener，文档 ⑥ 第 1 条需更新"; fail=$((fail+1))
else
  echo "  ok    05: 事务提交前发布的问题仍存在，文档论断有效"; pass=$((pass+1))
fi
```

```bash
assert_doc "docs/tech/05-异步链路与最终一致性.md"
```

- [ ] **Step 4: 运行核验**

```bash
./scripts/verify-docs.sh
```

预期：累计 39 项 ok，失败 0 项。

- [ ] **Step 5: 提交**

```bash
git add docs/tech/05-异步链路与最终一致性.md scripts/verify-docs.sh
git commit -m "docs: add teaching doc on async pipeline and eventual consistency"
```

---

### Task 6: 06-SuperMemo-2 间隔重复算法

**Files:**
- Create: `docs/tech/06-SM-2间隔重复算法.md`
- Modify: `scripts/verify-docs.sh`
- Read-only 素材: `common/utils/SM2Algorithm.java`、`module/spaced/service/impl/SpacedServiceImpl.java`（377 行）

**Interfaces:**
- Consumes: 无。
- Produces: 无。本篇可独立于其余各篇阅读。

**已核实的事实:**

| 事实 | 位置 |
| :--- | :--- |
| `MIN_EF = 1.3`、`INITIAL_EF = 2.5`、`FIRST_INTERVAL = 1`、`SECOND_INTERVAL = 6` | `SM2Algorithm` 常量 |
| EF 递推式 `EF' = EF + (0.1 - (5-q)*(0.08 + (5-q)*0.02))` | `calculateEasinessFactor()` |
| 全程用 `BigDecimal`，EF 结果 `setScale(2, HALF_UP)` | 同上 |
| EF 下限保护：低于 1.3 则钳到 1.3 | 同上 |
| **EF 更新在 `q < 3` 判断之前执行**，即评分再低 EF 也会更新 | `calculate()` |
| `q < 3` 时重置：`repetitionCount = 0`、`interval = FIRST_INTERVAL(1)` | `calculate()` |
| 间隔：`n=1 → 1`，`n=2 → 6`，`n≥3 → 上次间隔 × EF`（`setScale(0, HALF_UP)` 取整） | `calculateInterval()` |
| `calculateInterval` 传入的是**新算出的 EF**，非旧 EF | `calculate()` 调用处 |
| `quality` 越界（<0 或 >5）抛 `IllegalArgumentException` | `calculate()` 开头 |
| 下次复习时间 = `LocalDateTime.now().plusDays(interval)` | `calculate()` 末尾 |
| `getInitialState()`：count=0、interval=0、EF=2.5、nextReview = `now().minusMinutes(1)`（保证初始化后立即可复习） | `getInitialState()` |

- [ ] **Step 1: 核对素材并实机验算**

```bash
sed -n '35,145p' src/main/java/com/ray/knowledgeflow/common/utils/SM2Algorithm.java
sed -n '145,200p' src/main/java/com/ray/knowledgeflow/common/utils/SM2Algorithm.java
grep -n "SM2Algorithm\|calculate(" src/main/java/com/ray/knowledgeflow/module/spaced/service/impl/SpacedServiceImpl.java
```

手工验算三组数据，写进文档 ② 段的表格。用 EF 公式 `EF' = EF + (0.1 - (5-q)*(0.08+(5-q)*0.02))` 逐个算，**算完对照代码的 `setScale(2, HALF_UP)` 确认小数位**：

- q=5：diff=0 → adjustment = 0.1 − 0 = +0.10 → EF: 2.5 → 2.60
- q=4：diff=1 → adjustment = 0.1 − 1×(0.08+0.02) = 0.1 − 0.10 = 0.00 → EF: 2.5 → 2.50
- q=3：diff=2 → adjustment = 0.1 − 2×(0.08+0.04) = 0.1 − 0.24 = −0.14 → EF: 2.5 → 2.36
- q=2：diff=3 → adjustment = 0.1 − 3×(0.08+0.06) = 0.1 − 0.42 = −0.32 → EF: 2.5 → 2.18
- q=0：diff=5 → adjustment = 0.1 − 5×(0.08+0.10) = 0.1 − 0.90 = −0.80 → EF: 2.5 → 1.70

**写入文档前必须自己重算一遍确认。**

- [ ] **Step 2: 写文档**

**开篇必须先澄清命名**（在标题下方第一段）：

> `common/utils/SM2Algorithm.java` 中的 SM2 指 **SuperMemo-2**，一种间隔重复算法，与国密 SM2 椭圆曲线密码算法无关。本项目未使用国密算法。

**① 这个问题为什么难** — 场景：

> 用户有 300 条笔记。今天该复习哪些？「全部过一遍」不现实；「按创建时间轮流」会让已经烂熟的内容和刚学的内容占用同样多的次数。真正想要的是：**在快要忘掉的那一刻复习**——太早是浪费，太晚就得重学。

难点在于「快要忘掉」这个时刻**因人因材料而异**，且**随复习次数变化**（同一条笔记复习三次后，遗忘速度会明显变慢）。系统只能观测到一个信号：用户这次回忆得怎么样。要从这一个反馈信号反推出下次该隔多久。

**② 原理** — 分四步讲：

1. **遗忘曲线与间隔效应。** 记忆留存随时间衰减；每成功回忆一次，曲线变平缓，下次可以隔更久。这就是间隔递增的依据。
2. **两个状态变量。** SM-2 用两个量刻画一条材料的记忆状态：
   - `interval`（间隔天数）：距离下次复习多久。
   - `EF`（easiness factor，难度因子）：这条材料对这个用户有多容易。**关键点：`interval` 是短期状态，每次复习都可能重置；`EF` 是长期状态，跨越重置持续累积。** 这是 SM-2 设计上最精妙的一点——即使用户忘了一次导致间隔清零，系统仍记得「这条材料对你偏难」，重新爬升时会爬得更慢。
3. **EF 的递推式。** `EF' = EF + (0.1 - (5-q)·(0.08 + (5-q)·0.02))`。把 `d = 5 - q` 代入看清结构：调整量 = `0.1 - d·(0.08 + 0.02d)`，是关于 d 的二次函数，开口向下、随 d 增大而快速变负。用 Step 1 验算的表格呈现：

   | q | 含义 | 调整量 | EF 从 2.5 变为 |
   | :-: | :--- | :-: | :-: |
   | 5 | 完美回忆 | +0.10 | 2.60 |
   | 4 | 略有迟疑 | 0.00 | 2.50 |
   | 3 | 勉强想起 | −0.14 | 2.36 |
   | 2 | 想不起 | −0.32 | 2.18 |
   | 0 | 完全遗忘 | −0.80 | 1.70 |

   读出两个设计意图：**q=4 是中性点**（不奖不惩），只有完美回忆才涨 EF；**惩罚远重于奖励**（q=0 扣 0.80，q=5 只加 0.10），算法对遗忘敏感、对熟练保守。

4. **间隔递推与两个边界。**
   - `n=1 → 1 天`，`n=2 → 6 天`，`n≥3 → 上次间隔 × EF`。前两个是固定的经验值（不由 EF 决定），从第三次起才进入指数增长。
   - **EF 下限 1.3**：这是防止指数退化成常数。若 EF 可以降到 1.0，`interval × 1.0 = interval`，间隔永远不增长，用户会被同一条难材料无限纠缠。1.3 保证最难的材料也仍在增长，只是慢。
   - **q < 3 重置**：回忆失败说明前面积累的间隔判断错了，`repetitionCount` 归零、间隔退回 1 天，从头再爬。**但 EF 不重置**——这正是第 2 点说的长期/短期分离。

**③ 代码走读**（用 Step 1 的真实行号）：

- 四个常量（约 8 行）：与 ② 的 `MIN_EF`、`INITIAL_EF`、两个固定间隔对应。
- `calculate()` 主流程（约 20 行）：**重点指出执行顺序**——`calculateEasinessFactor` 在 `q < 3` 判断**之前**调用，所以评分再低 EF 也会更新。这与 ② 第 4 点「EF 不重置」是同一件事在代码上的体现，容易被误实现成「q<3 就整个重置」。
- `calculateEasinessFactor()`（约 18 行）：`BigDecimal` 逐步展开公式。讲清为什么用 `BigDecimal` 而非 `double`——EF 是**跨次累积**的状态，每次复习都在上次结果上继续加减，`double` 的二进制浮点误差会随复习次数累积；而 EF 又直接乘进间隔天数，误差最终会体现为复习日期的偏移。`setScale(2, HALF_UP)` 把 EF 固定在两位小数，让状态可精确复现。
- 下限钳制（约 3 行）：`newEF.compareTo(MIN_EF) < 0` → 钳到 1.3。
- `calculateInterval()`（约 12 行）：三分支。注意 `n≥3` 分支传入的是**新 EF**（调用处 `calculateInterval(newRepetitionCount, intervalDays, newEF)`），即本次评分立即影响本次间隔，而非下次才生效。
- `getInitialState()`（约 10 行）：`nextReviewTime = now().minusMinutes(1)`。讲清这个小技巧——设成「1 分钟前」而非「现在」，是为了让「查询到期卡片」的 `where next_review_time <= now()` 条件立刻成立，避免边界上的相等比较和时钟精度问题。

**④ 为什么不用 X 而用 Y**：

| 方案 | 取舍 |
| :--- | :--- |
| 固定间隔（1/3/7/15/30 天） | 实现最简，但对所有材料一视同仁，学得快的被反复打扰、学不会的又跟不上 |
| SM-2（本项目） | 两个状态变量、纯函数、无需训练数据，冷启动即可用 |
| SM-17 / SM-18 | SuperMemo 后续版本，引入更完整的记忆模型，精度更高但复杂度和数据需求大幅上升 |
| FSRS（现代主流，Anki 已采用） | 基于大规模真实复习数据拟合的记忆模型，效果优于 SM-2。**代价是需要参数和训练数据支撑**，个人项目冷启动阶段没有数据可拟合 |

诚实结论：SM-2 是 1987 年的算法，**已经不是当前最优**。选它的理由是零数据依赖、可解释、几十行能实现完。这个结论要主动说——比硬夸 SM-2 先进可信得多。

**⑤ 追问预演**：

- 第一层：「EF 下限为什么是 1.3？」
  → 讲清若 EF 可低于 1，间隔不再增长甚至缩短，用户被难材料锁死。1.3 是 SuperMemo 原始论文给的经验下限。
- 第二层：「q < 3 时你把 repetitionCount 和间隔都重置了，但 EF 没重置。为什么不一起重置？」
  → 这是本篇的核心考点。讲长期/短期状态分离：间隔反映「当前这轮爬到哪了」，EF 反映「这条材料对你有多难」。忘了一次说明这轮的间隔判断错了，该重爬；但「这条材料难」这个认知是正确的，应当保留，让重爬时爬得更慢。**如果连 EF 一起重置，用户每次忘掉都会回到「假设这条材料中等难度」，算法永远学不会这条材料对他而言很难。**
- 第三层：「你的 `nextReviewTime` 是 `LocalDateTime.now().plusDays(interval)`。假设一张卡片本该 7 月 1 日复习，用户 7 月 10 日才打开。他答对了，你算出下次间隔 15 天，下次复习日是 7 月 25 日。但从『上次复习日』算本该是 7 月 16 日。迟到的 9 天你没有任何处理——这会怎么样？」
  → **这是真实缺陷，当前实现完全没处理。** 两个后果：(a) 复习日期整体随用户的拖延持续后移，间隔被无意中拉长；(b) 更本质的是，用户隔了 16 天还能答对，说明这条材料比 EF 记录的更容易，标准 SM-2 之后的改进版会把「实际间隔」纳入 EF 计算，本实现没有。
  另外还有时区问题：`LocalDateTime.now()` 取服务器本地时区，跨时区用户的「今天该复习什么」会错位。改进：存 `Instant` 或 UTC，展示时按用户时区转换。

**⑥ 现有实现的坑与改进方向**：

1. **迟到复习未处理**（⑤ 第三层）：下次时间从 `now()` 而非计划到期日推算，且实际间隔未反馈进 EF。
2. **`LocalDateTime.now()` 依赖服务器时区**，跨时区用户的到期判断会错位。应改用 `Instant`/UTC。
3. **`now()` 直接写死在算法内部**，无法注入时钟，导致 `calculate()` 无法对「下次复习时间」做确定性单元测试。改进：传入 `Clock`。
4. **q < 3 时间隔重置为 1 而非 0**，与 `getInitialState()` 的 `interval = 0` 不一致——同样是「从头开始」，两条路径的初始间隔不同。需在文档中说明这个不一致，或指出应当统一。
5. **`quality` 用裸 `int` 且靠运行时抛异常校验**，用枚举（如 `AGAIN/HARD/GOOD/EASY`）能在编译期挡住非法值，也更贴合前端实际只有几个按钮的事实。
6. **算法已被 FSRS 超越**（见 ④），若后续积累了足够的真实复习记录，可考虑迁移。

- [ ] **Step 3: 追加断言**

```bash
SM2=src/main/java/com/ray/knowledgeflow/common/utils/SM2Algorithm.java
assert_code "$SM2" 'MIN_EF = new BigDecimal("1.3")'     "06: EF 下限仍为 1.3"
assert_code "$SM2" 'INITIAL_EF = new BigDecimal("2.5")' "06: EF 初值仍为 2.5"
assert_code "$SM2" 'FIRST_INTERVAL = 1'                 "06: 首次间隔仍为 1 天"
assert_code "$SM2" 'SECOND_INTERVAL = 6'                "06: 第二次间隔仍为 6 天"
assert_code "$SM2" 'if (quality < 3)'                   "06: q<3 重置分支仍存在"
assert_code "$SM2" 'LocalDateTime.now().plusDays'       "06: 下次复习时间仍从 now() 推算"
assert_code "$SM2" 'now().minusMinutes(1)'              "06: 初始状态仍设为 1 分钟前"
```

```bash
assert_doc "docs/tech/06-SM-2间隔重复算法.md"
```

- [ ] **Step 4: 运行核验**

```bash
./scripts/verify-docs.sh
```

预期：累计 47 项 ok，失败 0 项。

- [ ] **Step 5: 提交**

```bash
git add docs/tech/06-SM-2间隔重复算法.md scripts/verify-docs.sh
git commit -m "docs: add teaching doc on SuperMemo-2 spaced repetition"
```

---

### Task 7: 00-总览与索引，收口

**Files:**
- Create: `docs/tech/00-总览-架构与数据流.md`
- Modify: `README.md`（末尾追加文档索引链接）
- Modify: `scripts/verify-docs.sh`
- Delete: `docs/tech/.gitkeep`
- Read-only 素材: 前六篇产出 + `GraphServiceImpl.java`（407 行）、`GraphSyncListener.java`

**Interfaces:**
- Consumes: 01-06 全部结论。本篇是收口，**不重复论证**已在前六篇讲过的内容，只做串联与索引。
- Produces: 面向读者的入口。

**本篇与前六篇的分工**：前六篇是「纵深」，本篇是「横切」。本篇唯一独有的内容是 **Neo4j 双向链接** 一节（其余各篇均未覆盖）。

- [ ] **Step 1: 核对 Neo4j 素材**

```bash
grep -n "public " src/main/java/com/ray/knowledgeflow/module/graph/service/impl/GraphServiceImpl.java | head -20
grep -rn "\\[\\[" src/main/java/com/ray/knowledgeflow/module/note/service/impl/NoteServiceImpl.java | head
grep -rn "Pattern.compile" src/main/java/com/ray/knowledgeflow/module/note/ | head
```

找到 `[[链接]]` 语法的解析位置（正则在哪、出链如何提取），记录真实 `file:line`。若解析逻辑不在 `NoteServiceImpl`，扩大搜索范围：

```bash
grep -rln "outgoingLinks\|OutgoingLinks" src/main/java
```

- [ ] **Step 2: 写总览文档**

结构与前六篇不同（总览不套六段式），采用如下五节：

**一、五种存储各自在解决什么**

表格，每种存储一行，回答「为什么它不能被 MySQL 替代」：

| 存储 | 承担 | 为什么不能用 MySQL 替代 |
| :--- | :--- | :--- |
| MySQL | 用户、笔记正文、评论、点赞 | 事务与强一致的归属，是唯一的事实来源 |
| Elasticsearch | 分词倒排索引 | MySQL 的 `LIKE '%x%'` 无法分词、无法算相关性得分、无法高亮 |
| Qdrant | 向量近邻检索 | 高维向量的近似最近邻检索需要专门索引结构（HNSW 等），SQL 引擎没有 |
| Neo4j | 笔记引用关系 | 「A 引用了谁、谁引用了 A、两跳之内还有谁」在 SQL 里是递归自连接，写起来和跑起来都不划算 |
| Redis | 计数、缓存 | 高频读写的计数落 MySQL 会打爆行锁 |

明确标注**代价**：五种存储意味着五份数据要同步，一致性问题全部转移到了同步链路上——这正是 05 篇的主题。这一节要把「多存储」讲成一个**有代价的取舍**而不是一个亮点。

**二、一次「保存笔记」发生了什么**

一张 ASCII 时序图，串起全部链路（数据来自 05 篇已核实的事实）：

```
POST /notes
  │
  ├─ @Transactional 开始                        NoteServiceImpl:82
  │    ├─ noteEntryMapper.insert(note)          → MySQL
  │    ├─ publishEvent(NoteSavedEvent)          :110  ← 事务内发布（见 05 篇 ⑥.1）
  │    └─ 投递 3 条 MQ 任务 SUMMARY/TAG/GRAPH   :114-116
  ├─ 事务提交
  │
  └─ 返回用户（此时以下全部尚未完成）
       │
       ├─[Spring 事件·同进程]
       │   ├─ SearchSyncListener  → 写 ES 索引
       │   └─ GraphSyncListener   → 解析 [[链接]] → 写 Neo4j   ⚠ @Async，可能早于事务提交
       │
       └─[RabbitMQ·可跨进程]
           └─ AiProcessConsumer（Resilience4j 限流 5 QPS）
                ├─ SUMMARY → LLM 生成摘要
                ├─ TAG     → LLM 抽取标签
                └─ GRAPH   → 构建图谱关系
```

图上直接标出 05 篇发现的竞态，并链接到该篇。

**三、Neo4j 双向链接**（本篇独有内容）

- `[[笔记标题]]` 语法的解析位置与正则（用 Step 1 的真实 `file:line`）。
- 出链（outgoing）与反链（backlink）的区别：出链是解析本文得到的，反链是**反向查询**得到的——「谁引用了我」。讲清为什么这件事适合放图数据库：反链在关系表里是一次全表扫描或需要额外索引，在图里就是反向遍历一条边。
- `GraphSyncListener` 如何在笔记保存后同步节点与边（`syncNote(note, linkedIds)`）。
- **诚实说明**：`GraphServiceImpl` 主体是 Cypher 增删改查，本项目对图的使用停留在「存关系 + 前端可视化」，**没有用到图算法**（PageRank 找核心笔记、社区发现做主题聚类、最短路径找知识桥接等）。这是 Neo4j 能力远未被用满的地方，也是明确的改进方向。这一节不要吹「知识图谱」，把实际用到的能力边界说清楚。

**四、七篇文档索引**

表格：文件名 + 一句话说明它回答什么问题 + 建议阅读顺序（01→02→03 是 RAG 链路，需连读；04、05、06 可独立跳读）。

**五、已知缺陷汇总**

把六篇 ⑥ 段的问题汇总成一张表：`问题 | 所在文档 | 位置 | 严重度`。严重度按「影响正确性 / 影响可观测性 / 影响可维护性」三档分。

这一节让读者（和面试官）一眼看到你对自己代码的问题有系统性认识——**这是整套文档最有说服力的一页**。

- [ ] **Step 3: 在 README.md 末尾追加索引链接**

在 `README.md` 末尾追加（不修改任何既有内容）：

```markdown

---

## 📖 深入技术文档

若想了解各模块的实现原理、设计取舍与已知局限，见 [`docs/tech/`](docs/tech/00-总览-架构与数据流.md)。

该系列以代码为唯一事实来源，与本 README 的描述若有出入，以技术文档为准。
```

最后一句是必要的——README 里存在 OAuth2、1536 维等与代码不符的描述（见规格第 2 节），这句话给出了消歧规则。

- [ ] **Step 4: 追加断言并删除占位文件**

```bash
assert_doc_overview() {
  local doc="docs/tech/00-总览-架构与数据流.md"
  [[ -f "$doc" ]] || { echo "  FAIL  总览文档不存在"; fail=$((fail+1)); return; }
  local missing=""
  for h in "五种存储" "一次「保存笔记」" "Neo4j 双向链接" "文档索引" "已知缺陷汇总"; do
    grep -qF -- "$h" "$doc" || missing="$missing [$h]"
  done
  if [[ -n "$missing" ]]; then
    echo "  FAIL  总览缺少章节:$missing"; fail=$((fail+1)); return
  fi
  echo "  ok    总览结构完整"; pass=$((pass+1))
}
assert_doc_overview

# 七篇齐备
for f in 00-总览-架构与数据流 01-分块策略-从固定长度到语义切分 02-召回精排两段式检索 \
         03-自建重排服务与降级设计 04-混合检索与RRF融合 05-异步链路与最终一致性 \
         06-SM-2间隔重复算法; do
  if [[ -f "docs/tech/$f.md" ]]; then
    echo "  ok    存在 $f.md"; pass=$((pass+1))
  else
    echo "  FAIL  缺少 $f.md"; fail=$((fail+1))
  fi
done

# README 已接入索引
assert_code README.md 'docs/tech/00-总览-架构与数据流.md' "README 已链接技术文档索引"
```

```bash
rm docs/tech/.gitkeep
```

- [ ] **Step 5: 全量核验**

```bash
./scripts/verify-docs.sh
```

预期：全部 ok，`失败 0 项`，退出码 0。这是整个计划唯一的验收关口——**任何一条 FAIL 都必须以代码为准修正文档，不得修改断言来迁就文档。**

- [ ] **Step 6: 提交**

```bash
git add docs/tech/ README.md scripts/verify-docs.sh
git commit -m "docs: add architecture overview and wire up teaching doc index"
```

---

## 验收标准

全部任务完成后，逐条确认：

- [ ] `docs/tech/` 下 7 个 `.md` 文件齐备，无 `.gitkeep` 残留
- [ ] `./scripts/verify-docs.sh` 退出码 0，无 FAIL
- [ ] 01-06 每篇六段齐全，段落标题与 Global Constraints 中规定的文字一致
- [ ] 每篇 ⑥ 段至少一条带 `file:line` 的具体缺陷
- [ ] 规格第 2 节的四处文档偏差已澄清：OAuth2 未实现（00 篇或 README 说明）、SuperMemo-2 命名（06 篇开篇）、向量 2048 维（01 篇 ⑥）、代码能力强于旧文档（00 篇）
- [ ] 全文检索确认无营销词汇与占位符：
  ```bash
  grep -rnE 'TODO|TBD|待补充|颠覆性|极致|业界领先|深度赋能' docs/tech/ && echo "存在违规" || echo "干净"
  ```
- [ ] 全文检索确认无未标注的性能数字：
  ```bash
  grep -rnE '[0-9]+ ?(ms|毫秒|QPS|倍)' docs/tech/
  ```
  逐条人工确认每处要么有实测依据，要么明确标注了「未实测」。
