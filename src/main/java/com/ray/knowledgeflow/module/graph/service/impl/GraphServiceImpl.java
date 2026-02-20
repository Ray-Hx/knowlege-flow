package com.ray.knowledgeflow.module.graph.service.impl;

import com.ray.knowledgeflow.module.graph.entity.NoteNode;
import com.ray.knowledgeflow.module.graph.repository.GraphNoteRepository;
import com.ray.knowledgeflow.module.graph.service.IGraphService;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private final ChatLanguageModel chatModel;

    @Override
    @Async
    @Transactional
    public void syncNote(NoteEntry note, List<Long> linkedNoteIds) {
        log.info("Graph sync started: noteId={}", note.getId());

        try {
            // 1. 创建或获取当前节点
            NoteNode currentNode = graphRepository.findById(note.getId())
                    .orElse(new NoteNode(note.getId(), note.getTitle(), note.getUserId()));

            // 更新标题（如果变更）
            if (!note.getTitle().equals(currentNode.getTitle())) {
                currentNode.setTitle(note.getTitle());
            }

            // 2. 处理关系
            // 先删除旧关系（简单处理：先作为起点删除所有出边，或者根据业务需求增量更新）
            // 这里我们选择重新构建：清空 links，重新添加
            currentNode.getLinks().clear();

            // 3. 构建新关系
            if (linkedNoteIds != null && !linkedNoteIds.isEmpty()) {
                List<NoteNode> linkedNotes = graphRepository.findAllById(linkedNoteIds);
                // 此时 linkedNotes 可能不全（有些目标笔记还没同步到图谱），
                // 策略：如果目标笔记不在图谱中，是否要自动创建？
                // 简单起见：只链接已存在的节点，或者创建仅有ID的占位节点（推荐后者，保证关系完整）

                Set<Long> foundIds = new HashSet<>();
                for (NoteNode linked : linkedNotes) {
                    currentNode.addLink(linked);
                    foundIds.add(linked.getId());
                }

                // 对不存在的节点创建占位符（只有ID和默认标题）
                for (Long linkId : linkedNoteIds) {
                    if (!foundIds.contains(linkId)) {
                        // 注意：这里我们无法得知目标笔记的标题和userId，
                        // 除非查询 NoteEntry 表。鉴于这是异步方法，可以接受查询。
                        // 但为了性能，这里暂时只创建最小化节点，或者忽略（取决于业务一致性要求）。
                        // 假设：syncNote 是在笔记保存时触发，如果引用的笔记尚未同步，图谱中会有断层。
                        // 改进：创建仅ID节点，后续目标笔记同步时会更新属性。
                        // 注意 Neo4j OGM 保存行为：如果 ID 存在则更新，否则创建。
                        NoteNode placeholder = new NoteNode();
                        placeholder.setId(linkId);
                        // 标题暂时未知，设为空或ID
                        placeholder.setTitle("Note " + linkId);
                        // UserId 未知，可能导致权限过滤问题。
                        // 最好是在这里查询 NoteEntry 获取详情，或者由调用方传入更详细的 Link 信息。
                        // 为了简化，这里暂不自动创建缺失节点，只链接已有节点。
                        // 或者：调用方保证 linkedNoteIds 对应的节点应该存在。
                        log.warn("Target node not found for linking: from={} to={}", note.getId(), linkId);
                    }
                }
            }

            // 4. 保存
            graphRepository.save(currentNode);
            log.info("Graph sync completed: noteId={}", note.getId());

        } catch (Exception e) {
            log.error("Failed to sync note to graph: noteId={}", note.getId(), e);
        }
    }

    @Override
    @Async
    public void deleteNote(Long noteId) {
        log.info("Graph delete started: noteId={}", noteId);
        try {
            graphRepository.deleteById(noteId);
            log.info("Graph delete completed: noteId={}", noteId);
        } catch (Exception e) {
            log.error("Failed to delete note from graph: noteId={}", noteId, e);
        }
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
                            target.addRelated(rel.getTarget(), rel.getLabel());
                        }
                    }
                }
            }
        }

        return new java.util.ArrayList<>(nodeMap.values());
    }

    @Override
    public List<NoteNode> getGlobalGraph(int limit) {
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
                            target.addRelated(rel.getTarget(), rel.getLabel());
                        }
                    }
                }
            }
        }

        return new java.util.ArrayList<>(nodeMap.values());
    }

    @Override
    @Transactional // 移除 @Async，改为同步执行，以便返回结果
    public int analyzeRelations(Long userId) {
        return analyzeRelations(userId, msg -> {
        });
    }

    @Override
    @Transactional
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

        // 清理现有图谱数据，防止重复节点
        progressCallback.accept("{\"type\":\"progress\", \"current\":1, \"total\":4, \"message\":\"正在清理旧数据...\"}");
        try {
            graphRepository.deleteAllByUserId(userId);
            log.info("Cleared existing graph data for userId={}", userId);
        } catch (Exception e) {
            log.warn("Failed to clear graph data: {}", e.getMessage());
        }

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
            log.info("LLM batch response: {}", response);

            progressCallback.accept("{\"type\":\"progress\", \"current\":4, \"total\":4, \"message\":\"正在建立关联关系...\"}");

            // 4. 解析响应并建立关联
            int newRelations = 0;
            if (response != null && !response.trim().isEmpty() && !response.contains("无")) {
                String[] lines = response.split("\n");
                for (String line : lines) {
                    line = line.trim();
                    if (line.contains("->")) {
                        String[] parts = line.split("->");
                        if (parts.length == 2) {
                            try {
                                // 解析格式: sourceId->targetId:关系标签
                                String sourcePart = parts[0].replaceAll("[^0-9]", "");
                                String targetPart = parts[1];
                                String label = "相关"; // 默认标签

                                if (targetPart.contains(":")) {
                                    String[] targetLabel = targetPart.split(":", 2);
                                    targetPart = targetLabel[0].replaceAll("[^0-9]", "");
                                    label = targetLabel[1].trim();
                                } else {
                                    targetPart = targetPart.replaceAll("[^0-9]", "");
                                }

                                Long sourceId = Long.parseLong(sourcePart);
                                Long targetId = Long.parseLong(targetPart);

                                if (sourceId.equals(targetId))
                                    continue;
                                if (!noteMap.containsKey(sourceId) || !noteMap.containsKey(targetId))
                                    continue;

                                NoteEntry sourceNote = noteMap.get(sourceId);
                                NoteEntry targetNote = noteMap.get(targetId);

                                // 获取或创建 source 节点
                                NoteNode sourceNode = graphRepository.findById(sourceId)
                                        .orElse(new NoteNode(sourceId, sourceNote.getTitle(), sourceNote.getUserId()));

                                // 获取或创建 target 节点
                                NoteNode targetNode = graphRepository.findById(targetId).orElse(null);
                                if (targetNode == null) {
                                    targetNode = new NoteNode(targetId, targetNote.getTitle(), targetNote.getUserId());
                                    graphRepository.save(targetNode);
                                }

                                // 检查是否已链接
                                boolean linked = sourceNode.getLinks() != null && sourceNode.getLinks().stream()
                                        .anyMatch(l -> l.getId().equals(targetId));
                                boolean related = sourceNode.hasRelatedTo(targetId);

                                if (!linked && !related) {
                                    sourceNode.addRelated(targetNode, label);
                                    graphRepository.save(sourceNode);
                                    newRelations++;
                                    log.info("LLM Auto-link: {} -> {} ({})", sourceNote.getTitle(),
                                            targetNote.getTitle(), label);
                                }
                            } catch (NumberFormatException e) {
                                // 忽略无效行
                            }
                        }
                    }
                }
            }

            log.info("LLM Batch Analysis completed. Created {} new relations.", newRelations);
            progressCallback.accept(String.format("{\"type\":\"complete\", \"count\":%d}", newRelations));
            return newRelations;

        } catch (Exception e) {
            log.error("LLM batch analysis failed", e);
            progressCallback.accept("{\"type\":\"complete\", \"count\":0}");
            return 0;
        }
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
