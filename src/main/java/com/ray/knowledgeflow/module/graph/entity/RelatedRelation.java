package com.ray.knowledgeflow.module.graph.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

/**
 * AI关联关系属性（带标签）
 *
 * @author Ray
 * @since 2024-02-02
 */
@Data
@NoArgsConstructor
@RelationshipProperties
public class RelatedRelation {

    @Id
    @GeneratedValue
    private Long id;

    /**
     * 关系标签（如：技术依赖、相关主题、知识层级等）
     */
    private String label;

    @TargetNode
    private NoteNode target;

    public RelatedRelation(NoteNode target, String label) {
        this.target = target;
        this.label = label;
    }
}
