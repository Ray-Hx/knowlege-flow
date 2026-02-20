package com.ray.knowledgeflow.module.graph.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * 笔记图谱节点
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
@Node("SimpleNote")
@NoArgsConstructor
public class NoteNode {

    @Id
    private Long id; // 对应 MySQL 中的 note_id，不使用 @GeneratedValue

    private String title;

    private Long userId;

    @Relationship(type = "LINKS_TO", direction = Relationship.Direction.OUTGOING)
    private Set<NoteNode> links = new HashSet<>();

    @Relationship(type = "RELATED", direction = Relationship.Direction.OUTGOING)
    private Set<RelatedRelation> related = new HashSet<>();

    public NoteNode(Long id, String title, Long userId) {
        this.id = id;
        this.title = title;
        this.userId = userId;
    }

    public void addLink(NoteNode note) {
        if (links == null) {
            links = new HashSet<>();
        }
        links.add(note);
    }

    public void addRelated(NoteNode note, String label) {
        if (related == null) {
            related = new HashSet<>();
        }
        related.add(new RelatedRelation(note, label));
    }

    /**
     * 检查是否已存在与目标节点的 RELATED 关系
     */
    public boolean hasRelatedTo(Long targetId) {
        if (related == null)
            return false;
        return related.stream().anyMatch(r -> r.getTarget() != null && r.getTarget().getId().equals(targetId));
    }
}
