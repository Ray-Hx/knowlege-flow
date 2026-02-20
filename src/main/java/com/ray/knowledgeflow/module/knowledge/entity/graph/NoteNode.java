package com.ray.knowledgeflow.module.knowledge.entity.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.HashSet;
import java.util.Set;

/**
 * 知识图谱: 笔记节点
 */
@Data
@Node("Note")
public class NoteNode {

    @Id
    private Long noteId;

    private String title;

    private String summary;

    /**
     * 关系: REFERENCES (引用, 笔记 -> 笔记)
     */
    @Relationship(type = "REFERENCES", direction = Relationship.Direction.OUTGOING)
    private Set<NoteNode> references = new HashSet<>();

    /**
     * 关系: MENTIONS (提及, 笔记 -> 概念)
     */
    @Relationship(type = "MENTIONS", direction = Relationship.Direction.OUTGOING)
    private Set<ConceptNode> mentions = new HashSet<>();

    /**
     * 关系: TAGGED_WITH (标签, 笔记 -> 标签)
     */
    @Relationship(type = "TAGGED_WITH", direction = Relationship.Direction.OUTGOING)
    private Set<TagNode> tags = new HashSet<>();
}
