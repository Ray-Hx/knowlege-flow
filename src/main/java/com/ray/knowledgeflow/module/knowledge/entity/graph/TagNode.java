package com.ray.knowledgeflow.module.knowledge.entity.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 知识图谱: 标签节点
 */
@Data
@Node("Tag")
public class TagNode {

    @Id
    private String name;
}
