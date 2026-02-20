package com.ray.knowledgeflow.module.search.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.dto.ContentItemDto;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.note.dto.NoteListDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.entity.NoteFolder;
import com.ray.knowledgeflow.module.note.mapper.NoteFolderMapper;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.HighlightQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.data.elasticsearch.core.query.highlight.Highlight;
import org.springframework.data.elasticsearch.core.query.highlight.HighlightField;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
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

    @Override
    public Page<NoteListDto> searchNotes(String keyword, Long userId, int page, int size) {
        // 构建查询
        Query query = NativeQuery.builder()
                .withQuery(q -> q.bool(b -> b
                        .must(m -> m.term(t -> t.field("userId").value(userId)))
                        .must(m -> m.multiMatch(mm -> mm
                                .fields("title^3", "content", "summary^2", "tags")
                                .query(keyword)))))
                .withPageable(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedTime")))
                .withHighlightQuery(new HighlightQuery(new Highlight(List.of(
                        new HighlightField("title"),
                        new HighlightField("summary"),
                        new HighlightField("content"))), NoteDocument.class))
                .build();

        SearchHits<NoteDocument> searchHits = elasticsearchOperations.search(query, NoteDocument.class);

        List<NoteListDto> list = searchHits.stream().map(hit -> {
            NoteDocument doc = hit.getContent();
            NoteListDto dto = new NoteListDto();
            BeanUtils.copyProperties(doc, dto);

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
            if (doc.getFolderId() != null && doc.getFolderId() > 0) {
                NoteFolder folder = noteFolderMapper.selectById(doc.getFolderId());
                if (folder != null) {
                    dto.setFolderName(folder.getName());
                }
            }

            return dto;
        }).collect(Collectors.toList());

        return new PageImpl<>(list, PageRequest.of(page, size), searchHits.getTotalHits());
    }

    @Override
    public void syncNote(NoteEntry note) {
        try {
            NoteDocument doc = new NoteDocument();
            BeanUtils.copyProperties(note, doc);
            doc.setContent(note.getContentMd()); // 全文索引

            // 如果内容为空，尝试用summary填充
            if (doc.getContent() == null && note.getSummary() != null) {
                doc.setContent(note.getSummary());
            }

            noteRepository.save(doc);
            log.info("同步笔记索引成功: id={}", note.getId());
        } catch (Exception e) {
            log.error("同步笔记索引失败: id={}", note.getId(), e);
        }
    }

    @Override
    public void deleteNoteIndex(Long noteId) {
        try {
            noteRepository.deleteById(noteId);
            log.info("删除笔记索引成功: id={}", noteId);
        } catch (Exception e) {
            log.error("删除笔记索引失败: id={}", noteId, e);
        }
    }

    @Override
    public void syncContent(ContentItem content) {
        try {
            ContentDocument doc = new ContentDocument();
            BeanUtils.copyProperties(content, doc);
            // 转换Tags
            // if (content.getTags() != null) { ... }
            contentRepository.save(doc);
        } catch (Exception e) {
            log.error("同步内容索引失败: id={}", content.getId(), e);
        }
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

    @Override
    public Page<ContentItemDto> searchContentSemantic(String keyword, Long userId, int page, int size) {
        // 1. 生成查询向量
        List<Float> queryVector = embeddingService.embed(keyword);
        if (queryVector.isEmpty()) {
            return new PageImpl<>(Collections.emptyList());
        }

        try {
            // 2. Qdrant 向量检索
            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName("content")
                            .addAllVector(queryVector)
                            .setLimit(size)
                            .setOffset(page * size)
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            // TODO: 可以添加 Filter 过滤 userId，如果在 indexing 时存入了 userId
                            .build())
                    .get();

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

        } catch (InterruptedException | ExecutionException e) {
            log.error("语义搜索失败: keyword={}", keyword, e);
            Thread.currentThread().interrupt();
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
                    .supplyAsync(() -> {
                        try {
                            // 构建 ES 内容搜索查询
                            // 这里简化为直接调用 elasticsearchOperations，实际应复用或抽取方法
                            Query query = NativeQuery.builder()
                                    .withQuery(q -> q.bool(b -> b
                                            // .must(m -> m.term(t -> t.field("userId").value(userId))) //
                                            // 内容可能是公开的，或者根据需求过滤
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
                    });

            // 1.2 Qdrant 任务
            java.util.concurrent.CompletableFuture<List<Long>> vectorFuture = java.util.concurrent.CompletableFuture
                    .supplyAsync(() -> {
                        try {
                            List<Float> queryVector = embeddingService.embed(keyword);
                            if (queryVector.isEmpty())
                                return Collections.emptyList();

                            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                                    Points.SearchPoints.newBuilder()
                                            .setCollectionName("content")
                                            .addAllVector(queryVector)
                                            .setLimit(candidateSize)
                                            .setWithPayload(
                                                    Points.WithPayloadSelector.newBuilder().setEnable(false).build()) // 不需要
                                                                                                                      // payload，只要
                                                                                                                      // ID
                                            .build())
                                    .get();

                            return scoredPoints.stream().map(p -> p.getId().getNum()).collect(Collectors.toList());
                        } catch (Exception e) {
                            log.error("混合搜索 - 向量查询失败", e);
                            return Collections.emptyList();
                        }
                    });

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

            // 4. 排序并分页
            List<Long> sortedIds = rrfScores.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            int total = sortedIds.size();
            int start = page * size;
            int end = Math.min(start + size, total);

            if (start >= total) {
                return new PageImpl<>(Collections.emptyList(), PageRequest.of(page, size), total);
            }

            List<Long> pagedIds = sortedIds.subList(start, end);

            // 5. 抓取详情
            List<ContentItem> contentItems = contentItemMapper.selectList(
                    new LambdaQueryWrapper<ContentItem>().in(ContentItem::getId, pagedIds));

            Map<Long, ContentItem> itemMap = contentItems.stream()
                    .collect(Collectors.toMap(ContentItem::getId, i -> i));
            List<ContentItemDto> resultList = new ArrayList<>();

            for (Long id : pagedIds) {
                ContentItem item = itemMap.get(id);
                if (item != null) {
                    ContentItemDto dto = new ContentItemDto();
                    BeanUtils.copyProperties(item, dto);
                    resultList.add(dto);
                }
            }

            return new PageImpl<>(resultList, PageRequest.of(page, size), total);

        } catch (Exception e) {
            log.error("混合搜索失败: keyword={}", keyword, e);
            throw new RuntimeException("Hybrid search failed", e);
        }
    }

    @Override
    public List<Long> searchSimilarNoteIds(String text, int limit) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 截取前 500 个字符用于生成向量，避免超出模型长度
        String queryText = text.length() > 500 ? text.substring(0, 500) : text;
        List<Float> queryVector = embeddingService.embed(queryText);

        if (queryVector.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            List<Points.ScoredPoint> scoredPoints = qdrantClient.searchAsync(
                    Points.SearchPoints.newBuilder()
                            .setCollectionName("content_chunks")
                            .addAllVector(queryVector)
                            .setLimit(limit * 3) // 多取一些用于去重
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            .build())
                    .get();

            if (scoredPoints.isEmpty()) {
                return Collections.emptyList();
            }

            // 提取 sourceId (即笔记ID) 并去重，同时在内存中过滤 sourceType="NOTE"
            return scoredPoints.stream()
                    .filter(p -> {
                        io.qdrant.client.grpc.JsonWithInt.Value typeVal = p.getPayloadMap().get("sourceType");
                        return typeVal != null && typeVal.hasStringValue() && "NOTE".equals(typeVal.getStringValue());
                    })
                    .map(p -> {
                        io.qdrant.client.grpc.JsonWithInt.Value sourceIdVal = p.getPayloadMap().get("sourceId");
                        if (sourceIdVal != null && sourceIdVal.hasIntegerValue()) {
                            return sourceIdVal.getIntegerValue();
                        }
                        return null;
                    })
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .limit(limit)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("相似笔记搜索失败", e);
            return Collections.emptyList();
        }
    }
}
