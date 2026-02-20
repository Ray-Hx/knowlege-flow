package com.ray.knowledgeflow.module.knowledge.entity.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * 知识图谱: 概念节点
 */
@Data
@Node("Concept")
public class ConceptNode {

    @Id
    private String name;

    private String description;

    /**
     * 关系: RELATED_TO (相关, 概念 -> 概念)
     */
    @Relationship(type = "RELATED_TO", direction = Relationship.Direction.OUTGOING)
    private Set<ConceptNode> relatedConcepts = new HashSet<>();
}
