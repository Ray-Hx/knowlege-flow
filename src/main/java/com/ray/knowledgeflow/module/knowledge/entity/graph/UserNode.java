package com.ray.knowledgeflow.module.knowledge.entity.graph;

import lombok.Data;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

/**
 * 知识图谱: 用户节点
 */
@Data
@Node("User")
public class UserNode {

    @Id
    private Long userId;

    private String username;
}
