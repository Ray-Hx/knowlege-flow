#!/usr/bin/env bash
# 教学文档事实核验：确认文档中的代码论断与当前代码一致。
#
# 用法: ./scripts/verify-docs.sh
#
# 断言失败时的处理原则：以代码为准修正文档，不要改断言去迁就文档。
# 断言 FAIL 说明代码已经演进，而文档里的论断过期了。
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

# 断言文档存在且六段齐全、无占位符、无营销词汇。
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
  # 占位符检查只看散文部分：先剔除围栏代码块，再剔除行内代码。
  # 否则引用源码里真实存在的 TODO 注释会被误判。
  local prose
  prose=$(awk '/^```/{f=!f; next} !f' "$doc" | sed 's/`[^`]*`//g')
  if printf '%s' "$prose" | grep -qE 'TODO|TBD|待补充|FIXME'; then
    echo "  FAIL  $doc 散文中含占位符"; fail=$((fail+1)); return
  fi
  if grep -qE '颠覆性|极致|业界领先|深度赋能' "$doc"; then
    echo "  FAIL  $doc 含营销词汇"; fail=$((fail+1)); return
  fi
  echo "  ok    $doc 结构完整"; pass=$((pass+1))
}

echo "== 代码论断核验 =="

# ---- 01 分块策略 ----
CHUNKER=src/main/java/com/ray/knowledgeflow/module/ai/service/SemanticChunker.java
assert_code "$CHUNKER" 'breakpoint-percentile:85' "01: 分位数默认值仍为 85"
assert_code "$CHUNKER" 'min-chars:80'             "01: minChars 默认值仍为 80"
assert_code "$CHUNKER" 'max-chars:1000'           "01: maxChars 默认值仍为 1000"
assert_code "$CHUNKER" 'buffer-size:1'            "01: 滑动窗口 bufferSize 默认仍为 1"
assert_code "$CHUNKER" '1.0 - dot / (Math.sqrt(normA) * Math.sqrt(normB))' "01: 余弦距离公式未变"
assert_code "$CHUNKER" '(?<=[。！？；!?;])|(?<=\\.)(?=\\s)' "01: 句子边界正则未变（英文句点需后接空白）"
assert_code "$CHUNKER" 'fixedSizeFallback'        "01: 定长降级路径仍存在"
assert_code "$CHUNKER" 'embeddingService.embedAll' "01: 仍使用批量嵌入接口"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/ChunkingService.java \
            'createCollection(CHUNK_COLLECTION, 2048)' "01: 向量维度仍为 2048"
assert_code src/main/java/com/ray/knowledgeflow/module/search/service/EmbeddingService.java \
            'createCollection(collectionName, 2048)' "01: 默认建集合维度仍为 2048"

# ---- 02 召回精排 ----
RAG=src/main/java/com/ray/knowledgeflow/module/ai/service/RagRetriever.java
assert_code "$RAG" 'candidate-size:20'   "02: 召回候选数仍为 20"
assert_code "$RAG" 'top-k:5'             "02: 精排 topK 仍为 5"
assert_code "$RAG" 'score-threshold:0.5' "02: 相似度下限仍为 0.5"
assert_code "$RAG" 'content_chunks'      "02: 集合名仍为 content_chunks"
assert_code "$RAG" '.setFilter(userFilter(userId))' "02: 用户隔离仍下推到 Qdrant 侧"
assert_code "$RAG" 'RagContext.empty(query)'        "02: 检索失败降级路径仍存在"
assert_code "$RAG" 'Thread.currentThread().interrupt()' "02: 中断标志仍被恢复"
assert_code "$RAG" 'addedSourceIds.add(sourceId)'   "02: 引用列表仍按 sourceId 去重"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/Reranker.java \
            'List<Integer> rerank(String query, List<String> candidates, int topK)' \
            "02: 重排接口仍是「传字符串、返下标」"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/Reranker.java \
            'naturalOrder' "02: Reranker 降级静态方法仍存在"

# 反向断言：确认 payload 索引仍未创建（文档第六节第 1 条依赖此事实）
if grep -qE 'createPayloadIndex|createFieldIndex' \
     src/main/java/com/ray/knowledgeflow/module/search/service/EmbeddingService.java; then
  echo "  FAIL  02: 已创建 payload 索引，文档第六节第 1 条需更新"; fail=$((fail+1))
else
  echo "  ok    02: payload 索引仍未创建，文档论断有效"; pass=$((pass+1))
fi

# ---- 03 重排服务与降级 ----
CE=src/main/java/com/ray/knowledgeflow/module/ai/service/CrossEncoderReranker.java
assert_code "$CE" 'havingValue = "cross-encoder", matchIfMissing = true' "03: 交叉编码器仍为默认 provider"
assert_code "$CE" 'timeout-ms:3500'      "03: 重排超时默认仍为 3500ms"
assert_code "$CE" 'connectTimeout(Duration.ofSeconds(1))' "03: 连接超时仍为 1 秒"
assert_code "$CE" 'root.putArray("texts")' "03: 请求体仍双写 texts 字段"
assert_code "$CE" 'LinkedHashSet'        "03: 解析仍用 LinkedHashSet 去重保序"
assert_code "$CE" 'index >= 0 && index < candidateCount' "03: 下标越界过滤仍在"
assert_code "$CE" 'base-url:http://localhost:8080/rerank' "03: @Value 兜底仍是过期的 8080（第六节第 5 条）"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/LlmReranker.java \
            'Pattern.compile("\\[([-\\d,\\s]*)\\]")' "03: LLM 重排正则仍容忍负号"
assert_code src/main/java/com/ray/knowledgeflow/module/ai/service/NoOpReranker.java \
            'havingValue = "none"' "03: NoOp 重排器仍绑定 provider=none"
assert_code reranker-service/app.py 'BAAI/bge-reranker-v2-m3' "03: 重排模型未变"
assert_code reranker-service/app.py 'torch.inference_mode' "03: 推理仍用 inference_mode"
assert_code reranker-service/app.py 'RERANK_PORT", "8081"' "03: 重排服务仍监听 8081"
assert_code reranker-service/app.py 'logits.float().cpu()' "03: fp16 仍转回 fp32 出参"
assert_code src/main/resources/application.yml \
            'base-url: ${RERANK_BASE_URL:http://localhost:8081/rerank}' "03: yml 中 base-url 仍为正确的 8081"

# 反向断言：确认死配置块仍在（第六节第 7 条依赖此事实）
if grep -rq 'app\.ai\.rag\.chunk-size' src/main/java; then
  echo "  FAIL  03: app.ai.rag.chunk-size 已被代码读取，第六节第 7 条需更新"; fail=$((fail+1))
elif grep -q 'chunk-overlap: 50' src/main/resources/application.yml; then
  echo "  ok    03: 死配置块 app.ai.* 仍残留，文档论断有效"; pass=$((pass+1))
else
  echo "  FAIL  03: 死配置块已删除，第六节第 7 条需更新"; fail=$((fail+1))
fi

# ---- 04 混合检索与 RRF ----
SS=src/main/java/com/ray/knowledgeflow/module/search/service/impl/SearchServiceImpl.java
assert_code "$SS" 'final int k = 60; // RRF constant' "04: searchSimilarNoteIds 的 k 仍为 60"
assert_code "$SS" '1.0 / (k + i + 1)' "04: RRF 融合公式未变"
assert_code "$SS" 'Double::sum'       "04: 仍用 merge+sum 累加多路分数"
assert_code "$SS" 'title^3'           "04: ES 标题权重仍为 3"
assert_code "$SS" 'summary^2'         "04: ES 摘要权重仍为 2"
assert_code "$SS" 'Removed userId filter to recommend from whole community' "04: 推荐场景仍跨用户召回"
assert_code "$SS" 'int total = sortedIds.size();' "04: 混合搜索 total 仍取融合候选数"
assert_code "$SS" 'PageRequest.of(page, size), resultList.size())' "04: 语义搜索 total 仍取当页条数"
assert_code "$SS" 'Math.max(size * 2, 60)'  "04: 混合搜索候选数策略未变"
assert_code "$SS" 'Math.max(limit * 3, 50)' "04: 相似推荐候选数策略未变"
assert_code "$SS" 'noteRepository.deleteAll();'    "04: syncAll 仍先清空 Note 索引"
assert_code "$SS" 'contentRepository.deleteAll();' "04: syncAll 仍先清空 Content 索引"
assert_code "$SS" '.eq(ContentItem::getUserId, userId));' "04: 取详情时仍二次按 userId 收口"

# ---- 05 异步链路与最终一致性 ----
MQC=src/main/java/com/ray/knowledgeflow/config/RabbitMQConfig.java
assert_code "$MQC" '.deadLetterExchange(ENGAGEMENT_DLX)' "05: engagement 队列仍配置死信"
assert_code "$MQC" 'QueueBuilder.durable(AI_PROCESS_QUEUE).build()' "05: AI 队列仍无死信"
assert_code "$MQC" 'QueueBuilder.durable(LEARNING_REMINDER_QUEUE).build()' "05: 提醒队列仍无死信"
AIC=src/main/java/com/ray/knowledgeflow/module/ai/message/AiProcessConsumer.java
assert_code "$AIC" 'TODO: 可以考虑重试机制或发送到死信队列' "05: AI 消费者的 TODO 仍在"
assert_code "$AIC" '@RateLimiter(name = "ai-service")' "05: AI 消费者仍有限流注解"
NSI=src/main/java/com/ray/knowledgeflow/module/note/service/impl/NoteServiceImpl.java
assert_code "$NSI" '@Transactional(rollbackFor = Exception.class)' "05: createNote 仍带事务"
assert_code "$NSI" 'aiProcessProducer.sendSummaryTask' "05: 仍在事务内投递 AI 任务"
# 「好例子」仍在：点赞链路用 AFTER_COMMIT 延迟投递
assert_code src/main/java/com/ray/knowledgeflow/module/note/message/EngagementSyncProducer.java \
            '@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)' \
            "05: 点赞链路仍用 AFTER_COMMIT 投递（第三节正例）"

# 反向断言：图谱监听器仍未修复，第六节第 1 条依赖此事实
GSL=src/main/java/com/ray/knowledgeflow/module/graph/listener/GraphSyncListener.java
if grep -qF 'TransactionalEventListener' "$GSL"; then
  echo "  FAIL  05: GraphSyncListener 已改用 TransactionalEventListener，第六节第 1 条需更新"; fail=$((fail+1))
elif grep -qF '@Async' "$GSL" && grep -qF '@EventListener' "$GSL"; then
  echo "  ok    05: 图谱监听器仍是 @Async @EventListener，竞态论断有效"; pass=$((pass+1))
else
  echo "  FAIL  05: GraphSyncListener 注解已变，需复核第六节第 1 条"; fail=$((fail+1))
fi

# 反向断言：提醒队列仍无消费者
if grep -rlF 'LEARNING_REMINDER_QUEUE' src/main/java | grep -qv 'RabbitMQConfig.java'; then
  echo "  FAIL  05: learning.reminder 已有消费者，第六节第 6 条需更新"; fail=$((fail+1))
else
  echo "  ok    05: learning.reminder 仍无消费者，文档论断有效"; pass=$((pass+1))
fi

# ---- 06 SuperMemo-2 ----
SM2=src/main/java/com/ray/knowledgeflow/common/utils/SM2Algorithm.java
assert_code "$SM2" 'MIN_EF = new BigDecimal("1.3")'     "06: EF 下限仍为 1.3"
assert_code "$SM2" 'INITIAL_EF = new BigDecimal("2.5")' "06: EF 初值仍为 2.5"
assert_code "$SM2" 'FIRST_INTERVAL = 1'                 "06: 首次间隔仍为 1 天"
assert_code "$SM2" 'SECOND_INTERVAL = 6'                "06: 第二次间隔仍为 6 天"
assert_code "$SM2" 'if (quality < 3)'                   "06: q<3 重置分支仍存在"
assert_code "$SM2" 'setScale(2, RoundingMode.HALF_UP)'  "06: EF 仍保留两位小数"
assert_code "$SM2" 'LocalDateTime.now().plusDays'       "06: 下次复习时间仍从 now() 推算（第六节第 1 条）"
assert_code "$SM2" 'now().minusMinutes(1)'              "06: 初始状态仍设为 1 分钟前"
assert_code "$SM2" 'calculateInterval(newRepetitionCount, intervalDays, newEF)' \
            "06: 间隔计算仍传入新 EF"

# 反向断言：EF 更新必须早于 q<3 重置判断（第三节、第五节第二层的核心论断）
if [[ $(grep -n 'calculateEasinessFactor(quality, easinessFactor)' "$SM2" | head -1 | cut -d: -f1) \
      -lt $(grep -n 'if (quality < 3)' "$SM2" | head -1 | cut -d: -f1) ]]; then
  echo "  ok    06: EF 更新仍早于重置判断，长期/短期分离论断有效"; pass=$((pass+1))
else
  echo "  FAIL  06: EF 更新与重置判断的顺序已变，第三节论断需复核"; fail=$((fail+1))
fi

# ---- 00 总览（Neo4j 与双向链接为本篇独有内容）----
assert_code "$NSI" 'Pattern.compile("\\[\\[([^!][^\\]]*?)\\]\\]")' "00: 笔记链接正则仍排除 [[!article:]]"
assert_code "$NSI" '.like(NoteEntry::getContentMd, searchStr)' "00: 反链仍用 MySQL LIKE 子串匹配，未走 Neo4j"
assert_code src/main/java/com/ray/knowledgeflow/module/graph/service/impl/GraphServiceImpl.java \
            'public void syncNote(NoteEntry note, List<Long> linkedNoteIds)' "00: 图谱同步接口未变"

# 反向断言：OAuth2 仍未实现（总览末节的文档偏差表依赖此事实）
if [[ -n "$(ls -A src/main/java/com/ray/knowledgeflow/security/oauth/ 2>/dev/null)" ]] \
   || grep -rqF 'oauth2Login' src/main/java; then
  echo "  FAIL  00: OAuth2 已实现，总览末节的偏差表需更新"; fail=$((fail+1))
else
  echo "  ok    00: OAuth2 仍未实现，文档偏差表有效"; pass=$((pass+1))
fi

echo "== 文档结构核验 =="

assert_doc "docs/tech/01-分块策略-从固定长度到语义切分.md"
assert_doc "docs/tech/02-召回精排两段式检索.md"
assert_doc "docs/tech/03-自建重排服务与降级设计.md"
assert_doc "docs/tech/04-混合检索与RRF融合.md"
assert_doc "docs/tech/05-异步链路与最终一致性.md"
assert_doc "docs/tech/06-SM-2间隔重复算法.md"

# 总览不套六段式，单独核验其五个章节
assert_overview() {
  local doc="docs/tech/00-总览-架构与数据流.md"
  if [[ ! -f "$doc" ]]; then
    echo "  FAIL  总览文档不存在"; fail=$((fail+1)); return
  fi
  local missing=""
  for h in "五种存储各自在解决什么" "一次「保存笔记」发生了什么" \
           "Neo4j 与双向链接" "七篇文档索引" "已知缺陷汇总"; do
    grep -qF -- "$h" "$doc" || missing="$missing [$h]"
  done
  if [[ -n "$missing" ]]; then
    echo "  FAIL  总览缺少章节:$missing"; fail=$((fail+1)); return
  fi
  echo "  ok    总览结构完整"; pass=$((pass+1))
}
assert_overview

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

assert_code README.md 'docs/tech/00-总览-架构与数据流.md' "README 已链接技术文档索引"

echo
echo "通过 $pass 项，失败 $fail 项"
[[ $fail -eq 0 ]]
