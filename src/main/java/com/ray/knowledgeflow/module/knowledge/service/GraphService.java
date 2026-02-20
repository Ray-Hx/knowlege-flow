package com.ray.knowledgeflow.module.knowledge.service;

import com.ray.knowledgeflow.module.knowledge.entity.graph.NoteNode;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * 知识图谱服务
 *
 * @author Ray
 * @since 2024-01-21
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private final Neo4jTemplate neo4jTemplate;
    private final LinkParserService linkParserService;

    /**
     * 同步笔记到图谱
     *
     * @param note 笔记实体
     */
    @Async
    @Transactional
    public void syncNoteToGraph(NoteEntry note) {
        if (note == null)
            return;

        log.info("开始同步笔记到知识图谱: id={}, title={}", note.getId(), note.getTitle());

        try {
            // 1. 创建或更新当前笔记节点
            NoteNode noteNode = new NoteNode();
            noteNode.setNoteId(note.getId());
            noteNode.setTitle(note.getTitle());
            noteNode.setSummary(note.getSummary());

            // 2. 解析正文中的链接 [[Link]]
            Set<String> linkedTitles = linkParserService.extractWikiLinks(note.getContentMd());

            // 3. 建立关系 (REFERENCES)
            // 注意：Neo4j-OGM/SDN 如果关联的对象只设置了ID，可能不会自动查找并建立关系，
            // 这里为了简化，我们假设被引用的笔记可能还不存在于图中，或者我们只建立"指向标题"的弱引用。
            // 理想情况下，应该先查找被引用的笔记是否已存在对应的 NoteNode。
            // 这里我们采用一种策略：查找现有的 NoteNode，如果存在则建立关系。

            Set<NoteNode> references = new HashSet<>();
            for (String title : linkedTitles) {
                // 查找目标节点 (根据标题) - 这一步可能比较耗时，实际生产中可以优化
                String cypher = "MATCH (n:Note) WHERE n.title = $title RETURN n";
                NoteNode target = neo4jTemplate.findOne(cypher, java.util.Map.of("title", title), NoteNode.class)
                        .orElse(null);
                if (target != null) {
                    references.add(target);
                }
            }
            noteNode.setReferences(references);

            // 4. 保存 (save 会级联保存关系，但由于我们是从库里查出来的 target，应该没问题)
            neo4jTemplate.save(noteNode);

            log.info("同步笔记到知识图谱完成: id={}", note.getId());

        } catch (Exception e) {
            log.error("同步笔记到知识图谱失败: id={}", note.getId(), e);
        }
    }

    /**
     * 获取D3格式的图谱数据
     */
    public java.util.Map<String, Object> getGraphData() {
        // 查询所有节点和关系
        // 这里的 Cypher 查询返回所有 Note 节点及其关系
        String cypher = "MATCH (n:Note)-[r]->(m) RETURN n, r, m LIMIT 1000";
        // 注意: 直接使用 Neo4jTemplate 查询返回的是实体对象或 Map，我们需要转换为 D3 格式

        // 简化起见，我们先查询所有 Note 节点，再手动构建 D3 数据
        // 在生产环境中，应该使用更高效的 Cypher 查询直接返回 JSON 结构

        java.util.Collection<NoteNode> nodes = neo4jTemplate.findAll(NoteNode.class);

        java.util.List<java.util.Map<String, Object>> d3Nodes = new java.util.ArrayList<>();
        java.util.List<java.util.Map<String, Object>> d3Links = new java.util.ArrayList<>();
        java.util.Set<Long> addedNodeIds = new HashSet<>();

        for (NoteNode node : nodes) {
            if (!addedNodeIds.contains(node.getNoteId())) {
                d3Nodes.add(java.util.Map.of(
                        "id", node.getNoteId(),
                        "name", node.getTitle(),
                        "label", "Note"));
                addedNodeIds.add(node.getNoteId());
            }

            if (node.getReferences() != null) {
                for (NoteNode target : node.getReferences()) {
                    d3Links.add(java.util.Map.of(
                            "source", node.getNoteId(),
                            "target", target.getNoteId(),
                            "type", "REFERENCES"));

                    // 确保目标节点也存在于 nodes 列表中 (防止 isolated relationships 指向未知节点)
                    if (!addedNodeIds.contains(target.getNoteId())) {
                        d3Nodes.add(java.util.Map.of(
                                "id", target.getNoteId(),
                                "name", target.getTitle(), // 注意：这里可能 title 是空的，如果 target 只是一个关联对象
                                "label", "Note"));
                        addedNodeIds.add(target.getNoteId());
                    }
                }
            }
        }

        return java.util.Map.of("nodes", d3Nodes, "links", d3Links);
    }
}
