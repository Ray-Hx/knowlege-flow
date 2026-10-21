package com.ray.knowledgeflow.module.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.dto.ContentItemDto;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.note.dto.NoteListDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteFolder;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
import com.ray.knowledgeflow.module.ai.service.Reranker;
import com.ray.knowledgeflow.module.search.dto.SearchResultDto;
import com.ray.knowledgeflow.module.search.entity.ContentDocument;
import com.ray.knowledgeflow.module.search.entity.NoteDocument;
import com.ray.knowledgeflow.module.search.repository.ContentRepository;
import com.ray.knowledgeflow.module.search.repository.NoteRepository;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import com.ray.knowledgeflow.module.search.service.ISearchService;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightParameters;
import org.springframework.web.util.HtmlUtils;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * 搜索服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements ISearchService {

    private final NoteRepository noteRepository;
    private final ContentRepository contentRepository;
    private final NoteFolderMapper noteFolderMapper;
    private final ElasticsearchOperations elasticsearchOperations;
    private final EmbeddingService embeddingService;
    private final QdrantClient qdrantClient;
    private final ContentItemMapper contentItemMapper;
    private final NoteEntryMapper noteMapper;
    private final Reranker reranker;
    @Qualifier("searchTaskExecutor")
    private final Executor searchTaskExecutor;

    /** RRF 后送入精排的最小候选窗口。 */
    @Value("${app.search.hybrid.rerank-candidate-size:20}")
    private int hybridRerankCandidateSize = 20;

    @Value("${app.search.hybrid.timeout-ms:5000}")
    private long hybridTimeoutMs = 5000;

    @Value("${qdrant.operation-timeout-ms:5000}")
    private long qdrantOperationTimeoutMs = 5000;

    @Override
    public Page<NoteListDto> searchNotes(String keyword, Long userId, int page, int size) {
        // 构建查询
        Query query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.multiMatch(mm -> mm
                                .fields("title^3", "content", "summary^2", "tags")
                                .query(keyword)))
                        // 标题完整短语命中应明显优先于正文中的普通命中。
                        .should(s -> s.matchPhrase(mp -> mp
                                .field("title")
                                .query(keyword)
                                .boost(8.0f)))))
                // 不指定业务字段排序，让 Elasticsearch 按 _score 相关度排序。
                // 之前按 updatedTime 排序会完全覆盖上面的 title 权重。
                .withPageable(PageRequest.of(page, size))
                .withHighlightQuery(new HighlightQuery(safeHighlight(List.of(
                        new HighlightField("title"),
                        new HighlightField("summary"),
                        new HighlightField("content"))), NoteDocument.class))
                .build();

        SearchHits<NoteDocument> searchHits = elasticsearchOperations.search(query, NoteDocument.class);

        List<SearchHit<NoteDocument>> hitList = searchHits.stream().toList();
        Set<Long> folderIds = hitList.stream()
                .map(hit -> hit.getContent().getFolderId())
                .filter(id -> id != null && id > 0)
                .collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> folderNames = new HashMap<>();
        if (!folderIds.isEmpty()) {
            noteFolderMapper.selectList(new LambdaQueryWrapper<NoteFolder>()
                            .in(NoteFolder::getId, folderIds)
                            .eq(NoteFolder::getUserId, userId))
                    .forEach(folder -> folderNames.put(folder.getId(), folder.getName()));
        }

        List<NoteListDto> list = hitList.stream().map(hit -> {
            NoteDocument doc = hit.getContent();
            NoteListDto dto = new NoteListDto();
            BeanUtils.copyProperties(doc, dto);
            // 搜索页使用 v-html 呈现高亮，未命中的回退字段也必须转义。
            dto.setTitle(escapeHtml(doc.getTitle()));
            dto.setSummary(escapeHtml(doc.getSummary()));

            // 处理高亮
            List<String> titleHighlights = hit.getHighlightField("title");
            if (!titleHighlights.isEmpty()) {
                dto.setTitle(titleHighlights.get(0));
            }

            List<String> summaryHighlights = hit.getHighlightField("summary");
            if (!summaryHighlights.isEmpty()) {
                dto.setSummary(summaryHighlights.get(0));
            } else {
                List<String> contentHighlights = hit.getHighlightField("content");
                if (!contentHighlights.isEmpty()) {
                    dto.setSummary(contentHighlights.get(0));
                }
            }

            // 补充文件夹名称
            dto.setFolderName(folderNames.get(doc.getFolderId()));

            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(list, PageRequest.of(page, size), searchHits.getTotalHits());
    }

    @Override
    public List<SearchResultDto> globalSearch(String keyword, Long userId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }

        List<RankedGlobalResult> results = new ArrayList<>();

        // 1. 搜索个人笔记
        Query noteQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.multiMatch(mm -> mm
                                .fields("title^3", "content", "summary^2", "tags")
                                .query(keyword)))
                        .should(s -> s.matchPhrase(mp -> mp
                                .field("title")
                                .query(keyword)
                                .boost(8.0f)))))
                .withPageable(PageRequest.of(0, 10)) // 取 Top 10
                .withHighlightQuery(new HighlightQuery(safeHighlight(List.of(
                        new HighlightField("title"),
                        new HighlightField("summary"),
                        new HighlightField("content"))), NoteDocument.class))
                .build();

        SearchHits<NoteDocument> noteHits = elasticsearchOperations.search(noteQuery, NoteDocument.class);

        for (SearchHit<NoteDocument> hit : noteHits) {
            NoteDocument doc = hit.getContent();
            SearchResultDto dto = new SearchResultDto();
            dto.setId(String.valueOf(doc.getId()));
            dto.setType("NOTE");
            dto.setUpdateTime(doc.getUpdatedTime());

            // 标题高亮
            List<String> titleHighlights = hit.getHighlightField("title");
            dto.setTitle(!titleHighlights.isEmpty() ? titleHighlights.get(0) : escapeHtml(doc.getTitle()));

            // 摘要或正文高亮
            List<String> summaryHighlights = hit.getHighlightField("summary");
            if (!summaryHighlights.isEmpty()) {
                dto.setSnippet(summaryHighlights.get(0));
            } else {
                List<String> contentHighlights = hit.getHighlightField("content");
                if (!contentHighlights.isEmpty()) {
                    dto.setSnippet(contentHighlights.get(0));
                } else {
                    dto.setSnippet(escapeHtml(doc.getSummary()));
                }
            }
            results.add(new RankedGlobalResult(dto, hit.getScore()));
        }

        // 2. 搜索订阅内容
        Query contentQuery = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.multiMatch(mm -> mm
                                .fields("title^3", "content", "summary^2")
                                .query(keyword)))
                        .should(s -> s.matchPhrase(mp -> mp
                                .field("title")
                                .query(keyword)
                                .boost(8.0f)))))
                .withPageable(PageRequest.of(0, 10)) // 取 Top 10
                .withHighlightQuery(new HighlightQuery(safeHighlight(List.of(
                        new HighlightField("title"),
                        new HighlightField("summary"),
                        new HighlightField("content"))), ContentDocument.class))
                .build();

        SearchHits<ContentDocument> contentHits = elasticsearchOperations.search(contentQuery, ContentDocument.class);

        for (SearchHit<ContentDocument> hit : contentHits) {
            ContentDocument doc = hit.getContent();
            SearchResultDto dto = new SearchResultDto();
            dto.setId(String.valueOf(doc.getId()));
            dto.setType("CONTENT");
            dto.setUpdateTime(doc.getCreatedTime());

            // 标题高亮
            List<String> titleHighlights = hit.getHighlightField("title");
            dto.setTitle(!titleHighlights.isEmpty() ? titleHighlights.get(0) : escapeHtml(doc.getTitle()));

            // 摘要或正文高亮
            List<String> summaryHighlights = hit.getHighlightField("summary");
            if (!summaryHighlights.isEmpty()) {
                dto.setSnippet(summaryHighlights.get(0));
            } else {
                List<String> contentHighlights = hit.getHighlightField("content");
                if (!contentHighlights.isEmpty()) {
                    dto.setSnippet(contentHighlights.get(0));
                } else {
                    dto.setSnippet(escapeHtml(doc.getSummary()));
                }
            }
            results.add(new RankedGlobalResult(dto, hit.getScore()));
        }

        // 3. 相关性是搜索的主排序，时间仅在同分时作为稳定的次排序。
        // 之前混合后只按时间排序，会完全覆盖 ES/BM25 的 title boost 与 _score。
        results.sort((a, b) -> {
            int scoreOrder = Float.compare(b.score(), a.score());
            if (scoreOrder != 0) {
                return scoreOrder;
            }
            LocalDateTime aTime = a.result().getUpdateTime();
            LocalDateTime bTime = b.result().getUpdateTime();
            if (aTime == null) {
                return bTime == null ? 0 : 1;
            }
            return bTime == null ? -1 : bTime.compareTo(aTime);
        });

        // 最多返回 15 条
        return results.stream()
                .limit(15)
                .map(RankedGlobalResult::result)
                .toList();
    }

    private record RankedGlobalResult(SearchResultDto result, float score) {
    }

    private static Highlight safeHighlight(List<HighlightField> fields) {
        HighlightParameters parameters = HighlightParameters.builder()
                .withEncoder("html")
                .withPreTags("<em>")
                .withPostTags("</em>")
                .build();
        return new Highlight(parameters, fields);
    }

    private static String escapeHtml(String value) {
        return value == null ? "" : HtmlUtils.htmlEscape(value);
    }

    @Override
    public void syncNote(NoteEntry note) {
        NoteDocument doc = new NoteDocument();
        BeanUtils.copyProperties(note, doc);
        doc.setContent(note.getContentMd()); // 全文索引

        // 如果内容为空，尝试用summary填充
        if (doc.getContent() == null && note.getSummary() != null) {
            doc.setContent(note.getSummary());
        }

        noteRepository.save(doc);
        log.info("同步笔记索引成功: id={}", note.getId());
    }

    @Override
    public void deleteNoteIndex(Long noteId) {
        noteRepository.deleteById(noteId);
        log.info("删除笔记索引成功: id={}", noteId);
    }

    @Override
    public void syncContent(ContentItem content) {
        ContentDocument doc = new ContentDocument();
        BeanUtils.copyProperties(content, doc);
        // MySQL 字段名为 contentText，ES 文档字段名为 content，BeanUtils 不会自动映射。
        doc.setContent(content.getContentText());
        if (doc.getContent() == null && content.getSummary() != null) {
            doc.setContent(content.getSummary());
        }
        doc.setTags(parseContentTags(content));
        contentRepository.save(doc);
        log.info("同步内容索引成功: id={}", content.getId());
    }

    @Override
    public void deleteContentIndex(Long contentId) {
        contentRepository.deleteById(contentId);
        log.info("删除内容索引成功: id={}", contentId);
    }

    @Override
    public List<NoteDocument> searchDocs(String keyword, Long userId, int size) {
        Query query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.multiMatch(mm -> mm
                                .fields("title^3", "content", "summary^2", "tags")
                                .query(keyword)))))
                .withPageable(PageRequest.of(0, size))
                .build();

        SearchHits<NoteDocument> searchHits = elasticsearchOperations.search(query, NoteDocument.class);
        return searchHits.stream().map(SearchHit::getContent).collect(Collectors.toList());
    }

    /**
     * 构建 Qdrant 的用户维度过滤条件。
     *
     * <p>所有向量检索入口都必须带上，否则会检索到其他用户的私有内容。
     */
    private static Points.Filter userFilter(Long userId) {
        return Points.Filter.newBuilder()
                .addMust(Points.Condition.newBuilder()
                        .setField(Points.FieldCondition.newBuilder()
                                .setKey("userId")
                                .setMatch(Points.Match.newBuilder()
                                        .setInteger(userId)
                                        .build())
                                .build())
                        .build())
                .build();
    }

    @Override
    public Page<ContentItemDto> searchContentSemantic(String keyword, Long userId, int page, int size) {
        // 1. 生成查询向量
        List<Float> queryVector = embeddingService.embed(keyword);
        if (queryVector.isEmpty()) {
            return new PageImpl<>(Collections.emptyList());
        }

        try {
            // 2. Qdrant 向量检索，按 userId 过滤，避免检索到其他用户的内容
            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName("content")
                            .addAllVector(queryVector)
                            .setLimit(size)
                            .setOffset(page * size)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            .setFilter(userFilter(userId))
                            .build())
                    .get(qdrantOperationTimeoutMs, TimeUnit.MILLISECONDS);

            if (scoredPoints.isEmpty()) {
                return new PageImpl<>(Collections.emptyList());
            }

            // 3. 提取 ID 列表
            List<Long> contentIds = scoredPoints.stream()
                    .map(p -> p.getId().getNum())
                    .collect(Collectors.toList());

            // 4. 从数据库查询详细信息 (利用 MySQL 保持最新状态)
            List<ContentItem> contentItems = contentItemMapper.selectList(
                    new LambdaQueryWrapper<ContentItem>()
                            .in(ContentItem::getId, contentIds)
                            // 与向量过滤形成双重保险，防止 payload 缺失 userId 时越权
                            .eq(ContentItem::getUserId, userId)
            // 保持顺序需在内存处理，或者 field(id, ...)
            );

            // 5. 组装结果并保持向量检索的相关性顺序
            Map<Long, ContentItem> itemMap = contentItems.stream()
                    .collect(Collectors.toMap(ContentItem::getId, item -> item));

            List<ContentItemDto> resultList = new ArrayList<>();
            for (Points.ScoredPoint point : scoredPoints) {
                ContentItem item = itemMap.get(point.getId().getNum());
                if (item != null) {
                    ContentItemDto dto = new ContentItemDto();
                    BeanUtils.copyProperties(item, dto);
                    // 可以将相关度分数放入 dto (如果扩展了 DTO)
                    resultList.add(dto);
                }
            }

            // 这里的 total 只是这一页的数量或估算，向量数据库通常不返回精确 total
            return new PageImpl<>(resultList, PageRequest.of(page, size), resultList.size()); // Qdrant 不易获取 Total，暂时用
                                                                                              // list size

        } catch (InterruptedException e) {
            log.error("语义搜索失败: keyword={}", keyword, e);
            Thread.currentThread().interrupt();
            return new PageImpl<>(Collections.emptyList());
        } catch (ExecutionException | TimeoutException e) {
            log.error("语义搜索失败: keyword={}", keyword, e);
            return new PageImpl<>(Collections.emptyList());
        }
    }

    @Override
    public Page<ContentItemDto> searchContentHybrid(String keyword, Long userId, int page, int size) {
        // RRF 常数 k
        final int k = 60;
        // 获取更多候选集用于融合，例如取 requested size 的 2 倍或固定值 50-100
        final int candidateSize = Math.max(size * 2, 60);

        try {
            // 1. 并行执行关键字搜索 (ES) 和 语义搜索 (Qdrant)
            // 注意：这里为了复用逻辑，可能需要重构 searchNotes 为通用 searchContent (ES)，或者直接使用
            // contentRepository 查询 ES content index
            // 假设目前 ES 中也有 ContentDocument 索引

            // 1.1 ES 任务
            java.util.concurrent.CompletableFuture<List<Long>> esFuture = java.util.concurrent.CompletableFuture
                    .<List<Long>>supplyAsync(() -> {
                        try {
                            // 构建 ES 内容搜索查询
                            // 这里简化为直接调用 elasticsearchOperations，实际应复用或抽取方法
                            Query query = NativeQuery.builder()
                                    .withQuery(q -> q.bool(b -> b
                                            .must(m -> m.term(t -> t.field("userId").value(userId)))
                                            .must(m -> m.multiMatch(mm -> mm
                                                    .fields("title^3", "content", "summary^2", "tags")
                                                    .query(keyword)))))
                                    .withPageable(PageRequest.of(0, candidateSize)) // 取前N个
                                    .withSourceFilter(
                                            new org.springframework.data.elasticsearch.core.query.SourceFilter() {
                                                @Override
                                                public String[] getIncludes() {
                                                    return new String[] { "id" };
                                                }

                                                @Override
                                                public String[] getExcludes() {
                                                    return null;
                                                }
                                            })
                                    .build();

                            // 修正：查 ContentDocument
                            SearchHits<ContentDocument> docHits = elasticsearchOperations.search(query,
                                    ContentDocument.class);
                            return docHits.stream().map(h -> h.getContent().getId()).collect(Collectors.toList());
                        } catch (Exception e) {
                            log.error("混合搜索 - ES查询失败", e);
                            return Collections.emptyList();
                        }
                    }, searchTaskExecutor)
                    .completeOnTimeout(Collections.<Long>emptyList(), hybridTimeoutMs, TimeUnit.MILLISECONDS);

            // 1.2 Qdrant 任务
            java.util.concurrent.CompletableFuture<List<Long>> vectorFuture = java.util.concurrent.CompletableFuture
                    .<List<Long>>supplyAsync(() -> {
                        try {
                            List<Float> queryVector = embeddingService.embed(keyword);
                            if (queryVector.isEmpty())
                                return Collections.emptyList();

                            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                                    Points.SearchPoints.newBuilder()
                                            .setCollectionName("content")
                                            .addAllVector(queryVector)
                                            .setLimit(candidateSize)
                                            .setFilter(userFilter(userId))
                                            .setWithPayload(
                                                    Points.WithPayloadSelector.newBuilder().setEnable(false).build()) // 不需要
                                                                                                                      // payload，只要
                                                                                                                      // ID
                                            .build())
                                    .get(hybridTimeoutMs, TimeUnit.MILLISECONDS);

                            return scoredPoints.stream().map(p -> p.getId().getNum()).collect(Collectors.toList());
                        } catch (Exception e) {
                            log.error("混合搜索 - 向量查询失败", e);
                            return Collections.emptyList();
                        }
                    }, searchTaskExecutor)
                    .completeOnTimeout(Collections.<Long>emptyList(), hybridTimeoutMs, TimeUnit.MILLISECONDS);

            // 2. 等待结果
            java.util.concurrent.CompletableFuture.allOf(esFuture, vectorFuture).join();
            List<Long> esIds = esFuture.get();
            List<Long> vectorIds = vectorFuture.get();

            // 3. RRF 融合
            Map<Long, Double> rrfScores = new java.util.HashMap<>();

            // 处理 ES 结果
            for (int i = 0; i < esIds.size(); i++) {
                Long id = esIds.get(i);
                rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            }

            // 处理 向量 结果
            for (int i = 0; i < vectorIds.size(); i++) {
                Long id = vectorIds.get(i);
                rrfScores.merge(id, 1.0 / (k + i + 1), Double::sum);
            }

            // 4. RRF 只负责候选融合，再由交叉编码器完成最终精排
            List<Long> sortedIds = rrfScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            int total = sortedIds.size();
            int start = page * size;
            if (start >= total) {
                return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), total);
            }

            int requestedEnd = Math.min(start + size, total);
            // 精排必须在分页前完成。多取至少一条，避免候选数等于 topK
            // 时具体 Reranker 实现认为无需缩减而跳过网络调用。
            int rerankPoolSize = Math.min(total,
                    Math.max(hybridRerankCandidateSize, requestedEnd + 1));
            List<Long> rerankPoolIds = sortedIds.subList(0, rerankPoolSize);

            // 5. 取回精排文本（再次按 userId 收口，与检索侧形成双重保险）
            List<ContentItem> contentItems = contentItemMapper.selectList(
                    new LambdaQueryWrapper<ContentItem>()
                            .in(ContentItem::getId, rerankPoolIds)
                            .eq(ContentItem::getUserId, userId));

            Map<Long, ContentItem> itemMap = contentItems.stream()
                    .collect(Collectors.toMap(ContentItem::getId, i -> i));
            List<ContentItem> rrfOrderedItems = rerankPoolIds.stream()
                    .map(itemMap::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();

            if (rrfOrderedItems.isEmpty()) {
                return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), total);
            }

            List<String> rerankDocuments = rrfOrderedItems.stream()
                    .map(SearchServiceImpl::buildHybridRerankDocument)
                    .toList();

            int neededCount = Math.min(requestedEnd, rrfOrderedItems.size());
            int rerankTopK = neededCount;
            if (rerankDocuments.size() == rerankTopK && rerankTopK > 1) {
                // 请求 N-1 条即可推导完整排序：未返回的那一条放在最后。
                rerankTopK--;
            }

            List<Integer> rankedIndices = reranker.rerank(keyword, rerankDocuments, rerankTopK);
            List<ContentItem> rerankedItems = applyRerankOrder(rrfOrderedItems, rankedIndices);
            log.info("混合搜索精排: query={}, rrfCandidates={}, rerankPool={}, topK={}",
                    keyword, total, rerankDocuments.size(), rerankTopK);

            int pageEnd = Math.min(start + size, rerankedItems.size());
            if (start >= pageEnd) {
                return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), total);
            }

            List<ContentItemDto> resultList = rerankedItems.subList(start, pageEnd).stream()
                    .map(item -> {
                        ContentItemDto dto = new ContentItemDto();
                        BeanUtils.copyProperties(item, dto);
                        return dto;
                    })
                    .toList();

            return new PageImpl<>(resultList, PageRequest.of(page, size), total);

        } catch (Exception e) {
            log.error("混合搜索失败: keyword={}", keyword, e);
            throw new RuntimeException("Hybrid search failed", e);
        }
    }

    /**
     * 把标题、摘要和正文合成一个 query-document 精排文本。
     * 标题放在最前，避免长正文被模型截断时丢失最强相关性信号。
     */
    private static String buildHybridRerankDocument(ContentItem item) {
        StringBuilder document = new StringBuilder();
        appendRerankField(document, "标题", item.getTitle());
        appendRerankField(document, "摘要", item.getSummary());
        appendRerankField(document, "正文", item.getContentText());
        return document.toString();
    }

    private static void appendRerankField(StringBuilder document, String label, String value) {
        if (value != null && !value.isBlank()) {
            if (!document.isEmpty()) {
                document.append('\n');
            }
            document.append(label).append("：").append(value);
        }
    }

    /**
     * 先应用精排器返回的下标，再按 RRF 原顺序补齐未返回的候选。
     * 这样服务超时、降级或返回部分结果时仍不会丢数据。
     */
    private static List<ContentItem> applyRerankOrder(List<ContentItem> candidates, List<Integer> rankedIndices) {
        List<ContentItem> ordered = new ArrayList<>(candidates.size());
        boolean[] added = new boolean[candidates.size()];

        if (rankedIndices != null) {
            for (Integer index : rankedIndices) {
                if (index != null && index >= 0 && index < candidates.size() && !added[index]) {
                    ordered.add(candidates.get(index));
                    added[index] = true;
                }
            }
        }

        for (int i = 0; i < candidates.size(); i++) {
            if (!added[i]) {
                ordered.add(candidates.get(i));
            }
        }
        return ordered;
    }

    @Override
    public List<Long> searchSimilarNoteIds(String text, int limit) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        final int k = 60; // RRF constant
        final int candidateLimit = Math.max(limit * 3, 50);

        try {
            // 1. Elasticsearch Keyword Search (NoteDocument)
            java.util.concurrent.CompletableFuture<List<Long>> esFuture = java.util.concurrent.CompletableFuture
                    .<List<Long>>supplyAsync(() -> {
                        try {
                            Query query = NativeQuery.builder()
                                    .withQuery(q -> q.bool(b -> b
                                            .must(m -> m.term(t -> t.field("isPublic").value(1)))
                                            // Removed userId filter to recommend from whole community
                                            .must(m -> m.multiMatch(mm -> mm
                                                    .fields("title^3", "content", "summary^2", "tags")
                                                    .query(text)))))
                                    .withPageable(PageRequest.of(0, candidateLimit))
                                    .withSourceFilter(
                                            new org.springframework.data.elasticsearch.core.query.SourceFilter() {
                                                @Override
                                                public String[] getIncludes() {
                                                    return new String[] { "id" };
                                                }

                                                @Override
                                                public String[] getExcludes() {
                                                    return null;
                                                }
                                            })
                                    .build();

                            SearchHits<NoteDocument> hits = elasticsearchOperations.search(query, NoteDocument.class);
                            return hits.stream().map(h -> h.getContent().getId()).collect(Collectors.toList());
                        } catch (Exception e) {
                            log.error("Hybrid Similar Notes - ES search failed", e);
                            return Collections.emptyList();
                        }
                    }, searchTaskExecutor)
                    .completeOnTimeout(Collections.<Long>emptyList(), hybridTimeoutMs, TimeUnit.MILLISECONDS);

            // 2. Qdrant Semantic Search (content_chunks)
            java.util.concurrent.CompletableFuture<List<Long>> vectorFuture = java.util.concurrent.CompletableFuture
                    .<List<Long>>supplyAsync(() -> {
                        try {
                            String queryText = text.length() > 500 ? text.substring(0, 500) : text;
                            List<Float> queryVector = embeddingService.embed(queryText);
                            if (queryVector.isEmpty())
                                return Collections.emptyList();

                            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                                    Points.SearchPoints.newBuilder()
                                            .setCollectionName("content_chunks")
                                            .addAllVector(queryVector)
                                            .setLimit(candidateLimit)
                                            .setWithPayload(
                                                    Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                                            .build())
                                    .get(hybridTimeoutMs, TimeUnit.MILLISECONDS);

                            return scoredPoints.stream()
                                    .filter(p -> {
                                        io.qdrant.client.grpc.JsonWithInt.Value typeVal = p.getPayloadMap()
                                                .get("sourceType");
                                        return typeVal != null && typeVal.hasStringValue()
                                                && "NOTE".equals(typeVal.getStringValue());
                                    })
                                    .map(p -> {
                                        io.qdrant.client.grpc.JsonWithInt.Value sourceIdVal = p.getPayloadMap()
                                                .get("sourceId");
                                        return sourceIdVal != null && sourceIdVal.hasIntegerValue()
                                                ? (long) sourceIdVal.getIntegerValue()
                                                : null;
                                    })
                                    .filter(java.util.Objects::nonNull)
                                    .distinct()
                                    .collect(Collectors.toList());
                        } catch (Exception e) {
                            log.error("Hybrid Similar Notes - Qdrant search failed", e);
                            return Collections.emptyList();
                        }
                    }, searchTaskExecutor)
                    .completeOnTimeout(Collections.<Long>emptyList(), hybridTimeoutMs, TimeUnit.MILLISECONDS);

            // 3. Wait and Merge
            java.util.concurrent.CompletableFuture.allOf(esFuture, vectorFuture).join();
            List<Long> esIds = esFuture.get();
            List<Long> vectorIds = vectorFuture.get();

            // 4. RRF Fusion
            Map<Long, Double> rrfScores = new java.util.HashMap<>();
            for (int i = 0; i < esIds.size(); i++) {
                rrfScores.merge(esIds.get(i), 1.0 / (k + i + 1), Double::sum);
            }
            for (int i = 0; i < vectorIds.size(); i++) {
                rrfScores.merge(vectorIds.get(i), 1.0 / (k + i + 1), Double::sum);
            }

            // 5. Rank and Filter
            return rrfScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("混合相似笔记搜索失败", e);
            return Collections.emptyList();
        }
    }

    @Override
    public void syncAll() {
        log.info("开始全量同步数据到 Elasticsearch...");
        long startTime = System.currentTimeMillis();

        // 1. 同步 Note
        noteRepository.deleteAll(); // 清空原有 Note
        List<NoteEntry> notes = noteMapper.selectList(new LambdaQueryWrapper<>());
        int noteCount = 0;
        for (NoteEntry note : notes) {
            try {
                NoteDocument doc = new NoteDocument();
                BeanUtils.copyProperties(note, doc);
                doc.setContent(note.getContentMd());
                if (doc.getContent() == null && note.getSummary() != null) {
                    doc.setContent(note.getSummary());
                }
                noteRepository.save(doc);
                noteCount++;
            } catch (Exception e) {
                log.error("同步笔记失败: id={}", note.getId(), e);
            }
        }
        log.info("笔记全量同步完成，共 {} 条", noteCount);

        // 2. 同步 Content
        contentRepository.deleteAll(); // 清空原有 Content
        List<ContentItem> contents = contentItemMapper.selectList(new LambdaQueryWrapper<>());
        int contentCount = 0;
        for (ContentItem content : contents) {
            try {
                ContentDocument doc = new ContentDocument();
                BeanUtils.copyProperties(content, doc);
                doc.setContent(content.getContentText());
                if (doc.getContent() == null && content.getSummary() != null) {
                    doc.setContent(content.getSummary());
                }
                doc.setTags(parseContentTags(content));
                contentRepository.save(doc);
                contentCount++;
            } catch (Exception e) {
                log.error("同步内容失败: id={}", content.getId(), e);
            }
        }
        log.info("内容全量同步完成，共 {} 条", contentCount);

        log.info("Elasticsearch 全量同步完毕，耗时 {} ms", System.currentTimeMillis() - startTime);
    }

    private static List<String> parseContentTags(ContentItem content) {
        if (content.getTags() == null || content.getTags().isBlank()) {
            return Collections.emptyList();
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(
                    content.getTags(),
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {
                    });
        } catch (Exception e) {
            log.warn("解析 Content tags 失败: id={}", content.getId(), e);
            return Collections.emptyList();
        }
    }
}
