package com.ray.knowledgeflow.module.graph.service.impl;

import com.ray.knowledgeflow.module.graph.entity.NoteNode;
import com.ray.knowledgeflow.module.graph.repository.GraphNoteRepository;
import com.ray.knowledgeflow.module.graph.service.IGraphService;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.service.NoteLinkService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识图谱服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphServiceImpl implements IGraphService {

    private final GraphNoteRepository graphRepository;
    private final com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper noteEntryMapper;
    private final NoteLinkService noteLinkService;
    private final ChatLanguageModel chatModel;
    @Qualifier("neo4jTransactionTemplate")
    private final TransactionTemplate neo4jTransactionTemplate;

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager", rollbackFor = Exception.class)
    public void syncNote(NoteEntry note, List<Long> linkedNoteIds) {
        log.info("Graph sync started: noteId={}", note.getId());

        // 1. 创建或获取当前节点
        NoteNode currentNode = graphRepository.findById(note.getId())
                .orElse(new NoteNode(note.getId(), note.getTitle(), note.getUserId()));

        // 更新标题（如果变更）
        if (!note.getTitle().equals(currentNode.getTitle())) {
            currentNode.setTitle(note.getTitle());
        }
        currentNode.setUserId(note.getUserId());

        // 2. 只替换当前节点的显式出边，保留入边和 AI RELATED。
        currentNode.getLinks().clear();
        graphRepository.save(currentNode);
        graphRepository.deleteOutgoingLinks(note.getId());

        // 3. 构建新关系
        if (linkedNoteIds != null && !linkedNoteIds.isEmpty()) {
            for (Long linkId : linkedNoteIds) {
                NoteEntry target = noteEntryMapper.selectById(linkId);
                if (target == null || !note.getUserId().equals(target.getUserId())) {
                    log.warn("Target note missing or not owned by user: from={} to={}", note.getId(), linkId);
                    continue;
                }
                NoteNode linkedNode = graphRepository.findById(linkId)
                        .orElse(new NoteNode(target.getId(), target.getTitle(), target.getUserId()));
                linkedNode.setTitle(target.getTitle());
                linkedNode.setUserId(target.getUserId());
                graphRepository.save(linkedNode);
                currentNode.addLink(linkedNode);
            }
        }

        // 4. 保存。异常不在此处吞掉，由事务回滚并交给上层记录/重试。
        graphRepository.save(currentNode);
        log.info("Graph sync completed: noteId={}", note.getId());
    }

    @Override
    @Transactional(transactionManager = "neo4jTransactionManager", rollbackFor = Exception.class)
    public void deleteNote(Long noteId) {
        log.info("Graph delete started: noteId={}", noteId);
        graphRepository.deleteById(noteId);
        log.info("Graph delete completed: noteId={}", noteId);
    }

    @Override
    public int rebuildGraph(Long userId) {
        int count = noteLinkService.rebuildUserLinks(userId);
        List<NoteEntry> notes = noteEntryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteEntry>()
                        .eq(NoteEntry::getUserId, userId)
                        .orderByAsc(NoteEntry::getId));
        for (NoteEntry note : notes) {
            List<Long> targetIds = noteLinkService.getOutgoingTargetIds(userId, note.getId());
            // 类内直接调用不会经过 @Transactional 代理，因此显式包裹单篇投影。
            neo4jTransactionTemplate.executeWithoutResult(status -> syncNote(note, targetIds));
        }
        log.info("Graph explicit projection rebuilt: userId={}, notes={}", userId, count);
        return count;
    }

    @Override
    public List<NoteNode> getGraph(Long userId) {
        // 1. 获取用户所有笔记（MySQL）
        List<NoteEntry> allNotes = noteEntryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteEntry>()
                        .eq(NoteEntry::getUserId, userId)
        // 显示所有状态的笔记
        // 注意Schema中有 deleted 字段，MyBatis-Plus 自动处理逻辑删除
        );

        if (allNotes.isEmpty()) {
            return List.of();
        }

        // 2. 获取图谱关系（Neo4j）
        List<NoteNode> graphNodes = List.of();
        try {
            graphNodes = graphRepository.findGraphByUserId(userId);
        } catch (Exception e) {
            log.error("获取图谱关系失败 (Neo4j可能未连接): {}", e.getMessage());
            // 继续执行，只返回孤立节点
        }

        // 3. 融合数据：以 MySQL 数据为基准，填充 Neo4j 的关系
        java.util.Map<Long, NoteNode> nodeMap = new java.util.HashMap<>();

        // 先将 MySQL 笔记转换为 NoteNode（孤立状态）
        for (NoteEntry note : allNotes) {
            nodeMap.put(note.getId(), new NoteNode(note.getId(), note.getTitle(), note.getUserId()));
        }

        // 再将 Neo4j 的关系合并进来
        for (NoteNode graphNode : graphNodes) {
            if (nodeMap.containsKey(graphNode.getId())) {
                NoteNode target = nodeMap.get(graphNode.getId());
                // 合并 Links
                if (graphNode.getLinks() != null) {
                    for (NoteNode linked : graphNode.getLinks()) {
                        // 确保链接的目标节点也在 nodeMap 中
                        if (nodeMap.containsKey(linked.getId())) {
                            // Linked node from Neo4j might only have ID/Title.
                            // Using nodeMap.get is better to ensure consistency but addLink takes NoteNode.
                            // However, target.addLink(linked) adds the 'linked' object which is fine.
                            target.addLink(linked);
                        }
                    }
                }

                // 合并 Related (AI 关联)
                if (graphNode.getRelated() != null) {
                    for (com.ray.knowledgeflow.module.graph.entity.RelatedRelation rel : graphNode.getRelated()) {
                        if (rel.getTarget() != null && nodeMap.containsKey(rel.getTarget().getId())) {
                            // 使用保留 ID 的方式添加，防止 deduplication
                            target.addRelated(rel);
                        }
                    }
                }
            }
        }

        return new java.util.ArrayList<>(nodeMap.values());
    }

    @Override
    public List<NoteNode> getCommunityGraph(int limit) {
        // 1. 获取最新发布的公开笔记列表
        List<NoteEntry> publicNotes = noteEntryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteEntry>()
                        .eq(NoteEntry::getIsPublic, 1)
                        .eq(NoteEntry::getStatus, 2)
                        .orderByDesc(NoteEntry::getUpdatedTime)
                        .last("LIMIT " + limit));

        if (publicNotes.isEmpty()) {
            return List.of();
        }

        // 提取这些笔记的 ID 列表
        List<Long> publicIds = publicNotes.stream().map(NoteEntry::getId).collect(Collectors.toList());

        // 2. 从 Neo4j 获取子图关系
        List<NoteNode> graphNodes = List.of();
        try {
            graphNodes = graphRepository.findGraphByIds(publicIds);
        } catch (Exception e) {
            log.error("获取全网公开图谱关系失败 (Neo4j可能未连接): {}", e.getMessage());
        }

        // 3. 融合数据
        java.util.Map<Long, NoteNode> nodeMap = new java.util.HashMap<>();

        // 存入基础节点
        for (NoteEntry note : publicNotes) {
            nodeMap.put(note.getId(), new NoteNode(note.getId(), note.getTitle(), note.getUserId()));
        }

        // 融合 Neo4j 返回的关系
        for (NoteNode graphNode : graphNodes) {
            if (nodeMap.containsKey(graphNode.getId())) {
                NoteNode target = nodeMap.get(graphNode.getId());

                if (graphNode.getLinks() != null) {
                    for (NoteNode linked : graphNode.getLinks()) {
                        if (nodeMap.containsKey(linked.getId())) {
                            target.addLink(linked);
                        }
                    }
                }

                if (graphNode.getRelated() != null) {
                    for (com.ray.knowledgeflow.module.graph.entity.RelatedRelation rel : graphNode.getRelated()) {
                        if (rel.getTarget() != null && nodeMap.containsKey(rel.getTarget().getId())) {
                            target.addRelated(rel);
                        }
                    }
                }
            }
        }

        return new java.util.ArrayList<>(nodeMap.values());
    }

    @Override
    public int analyzeRelations(Long userId) {
        return analyzeRelations(userId, msg -> {
        });
    }

    @Override
    public int analyzeRelations(Long userId, java.util.function.Consumer<String> progressCallback) {
        log.info("Start LLM-based batch analyzing relations for userId={}", userId);

        // 1. 获取所有笔记 (不需要内容，只要标题和标签)
        List<NoteEntry> notes = noteEntryMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<NoteEntry>()
                        .eq(NoteEntry::getUserId, userId)
                        .eq(NoteEntry::getStatus, 2));

        int total = notes.size();
        if (total < 2) {
            progressCallback.accept("{\"type\":\"complete\", \"count\":0}");
            return 0;
        }

        // 显式 LINKS_TO 是用户数据，AI 分析绝不能删除；等模型成功返回后只替换 RELATED。
        progressCallback.accept("{\"type\":\"progress\", \"current\":1, \"total\":4, \"message\":\"正在读取现有关系...\"}");

        progressCallback.accept("{\"type\":\"progress\", \"current\":2, \"total\":4, \"message\":\"正在准备笔记数据...\"}");

        // 2. 构建笔记列表字符串 (ID: 标题 [标签])
        StringBuilder notesList = new StringBuilder();
        java.util.Map<Long, NoteEntry> noteMap = new java.util.HashMap<>();
        for (NoteEntry note : notes) {
            noteMap.put(note.getId(), note);
            String tags = note.getTags() != null ? " [" + note.getTags() + "]" : "";
            notesList.append(note.getId()).append(": ").append(note.getTitle()).append(tags).append("\n");
        }

        progressCallback.accept("{\"type\":\"progress\", \"current\":3, \"total\":4, \"message\":\"正在调用AI分析关联...\"}");

        // 3. 一次性让 LLM 分析所有笔记的关联关系
        String prompt = String.format(
                "你是一个知识关联分析助手。请分析以下笔记列表中哪些笔记之间存在知识关联关系。\n\n" +
                        "笔记列表:\n%s\n\n" +
                        "请找出所有有知识关联的笔记对（考虑:技术栈依赖,知识层级,相关主题等）。\n" +
                        "返回格式为每行一个关联,格式: 源ID->目标ID:关系描述\n" +
                        "关系描述为2-4个字,如'技术基础','框架依赖','同主题','进阶知识'等。\n" +
                        "例如:\n" +
                        "102->103:技术基础\n" +
                        "103->106:框架依赖\n" +
                        "104->105:进阶知识\n\n" +
                        "只返回关联对,不要包含任何解释。如果没有关联,返回\"无\"。",
                notesList.toString());

        try {
            // 单次 LLM 调用
            String response = chatModel.generate(prompt);
            log.debug("LLM batch relation response received, length={}", response == null ? 0 : response.length());

            progressCallback.accept("{\"type\":\"progress\", \"current\":4, \"total\":4, \"message\":\"正在建立关联关系...\"}");

            // 外部 LLM 调用不占用 Neo4j 事务；只在替换关系时开启短事务。
            List<RelatedPlan> relationPlans = parseRelatedPlans(response, noteMap);
            Integer written = neo4jTransactionTemplate.execute(
                    status -> replaceRelatedRelations(userId, noteMap, relationPlans));
            int newRelations = written == null ? 0 : written;

            log.info("LLM Batch Analysis completed. Created {} new relations.", newRelations);
            progressCallback.accept(String.format("{\"type\":\"complete\", \"count\":%d}", newRelations));
            return newRelations;

        } catch (Exception e) {
            log.error("LLM batch analysis failed", e);
            progressCallback.accept("{\"type\":\"error\", \"message\":\"关联分析失败\"}");
            throw new BusinessException(502, "AI 关联分析失败，原有关系已保留");
        }
    }

    private List<RelatedPlan> parseRelatedPlans(
            String response,
            java.util.Map<Long, NoteEntry> noteMap) {
        if (response == null || response.trim().isEmpty()) {
            throw new IllegalArgumentException("AI 返回了空的关联结果");
        }
        String normalized = response.trim();
        if ("无".equals(normalized)) {
            return List.of();
        }

        List<RelatedPlan> plans = new java.util.ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String rawLine : normalized.split("\n")) {
            String line = rawLine.trim();
            if (!line.contains("->")) {
                continue;
            }
            String[] parts = line.split("->", 2);
            try {
                String sourcePart = parts[0].replaceAll("[^0-9]", "");
                String targetPart = parts[1];
                String label = "相关";
                if (targetPart.contains(":")) {
                    String[] targetLabel = targetPart.split(":", 2);
                    targetPart = targetLabel[0].replaceAll("[^0-9]", "");
                    String candidate = targetLabel[1].trim();
                    if (!candidate.isEmpty()) {
                        label = candidate.substring(0, Math.min(candidate.length(), 20));
                    }
                } else {
                    targetPart = targetPart.replaceAll("[^0-9]", "");
                }

                Long sourceId = Long.parseLong(sourcePart);
                Long targetId = Long.parseLong(targetPart);
                String key = sourceId + "->" + targetId;
                if (!sourceId.equals(targetId)
                        && noteMap.containsKey(sourceId)
                        && noteMap.containsKey(targetId)
                        && seen.add(key)) {
                    plans.add(new RelatedPlan(sourceId, targetId, label));
                }
            } catch (NumberFormatException ignored) {
                // 继续尝试其他行，最终统一验证是否得到可用关系。
            }
        }

        if (plans.isEmpty()) {
            throw new IllegalArgumentException("AI 关联结果格式无效");
        }
        return plans;
    }

    private int replaceRelatedRelations(
            Long userId,
            java.util.Map<Long, NoteEntry> noteMap,
            List<RelatedPlan> relationPlans) {
        // 模型调用成功后，只替换 AI 推断关系，保留用户显式链接与全部节点。
        graphRepository.deleteRelatedByUserId(userId);

        int newRelations = 0;
        for (RelatedPlan plan : relationPlans) {
                Long sourceId = plan.sourceId();
                Long targetId = plan.targetId();
                NoteEntry sourceNote = noteMap.get(sourceId);
                NoteEntry targetNote = noteMap.get(targetId);
                NoteNode sourceNode = graphRepository.findById(sourceId)
                        .orElse(new NoteNode(sourceId, sourceNote.getTitle(), sourceNote.getUserId()));
                NoteNode targetNode = graphRepository.findById(targetId).orElse(null);
                if (targetNode == null) {
                    targetNode = new NoteNode(targetId, targetNote.getTitle(), targetNote.getUserId());
                    graphRepository.save(targetNode);
                }

                boolean linked = sourceNode.getLinks() != null && sourceNode.getLinks().stream()
                        .anyMatch(link -> link.getId().equals(targetId));
                if (!linked && !sourceNode.hasRelatedTo(targetId)) {
                    sourceNode.addRelated(targetNode, plan.label());
                    graphRepository.save(sourceNode);
                    newRelations++;
                }
        }
        return newRelations;
    }

    private record RelatedPlan(Long sourceId, Long targetId, String label) {
    }

    /**
     * 解析 LLM 返回的关联 ID 列表
     */
    private List<Long> parseRelatedIds(String response, Long excludeId) {
        List<Long> ids = new java.util.ArrayList<>();
        if (response == null || response.trim().isEmpty() || response.contains("无")) {
            return ids;
        }

        // 清理响应，只保留数字和逗号
        String cleaned = response.replaceAll("[^0-9,]", "");
        String[] parts = cleaned.split(",");

        for (String part : parts) {
            try {
                Long id = Long.parseLong(part.trim());
                if (!id.equals(excludeId) && id > 0) {
                    ids.add(id);
                }
            } catch (NumberFormatException e) {
                // 忽略无效 ID
            }
        }
        return ids;
    }

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
