package com.ray.knowledgeflow.module.graph.repository;

import com.ray.knowledgeflow.module.graph.entity.NoteNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 笔记图谱 Repository
 *
 * @author Ray
 * @since 2024-01-16
 */
@Repository
public interface GraphNoteRepository extends Neo4jRepository<NoteNode, Long> {

    /**
     * 获取用户的所有笔记节点（不包含关系，用于列表）
     */
    List<NoteNode> findAllByUserId(Long userId);

    /**
     * 获取指定用户的全量图谱（包含 LINKS_TO 和 RELATED 关系）
     * 使用 collect(DISTINCT n) 避免重复节点
     */
    @Query("MATCH (n:SimpleNote) WHERE n.userId = $userId OPTIONAL MATCH (n)-[r:LINKS_TO|RELATED]->(m:SimpleNote) WITH DISTINCT n, collect(DISTINCT r) as rels, collect(DISTINCT m) as targets RETURN n, rels, targets")
    List<NoteNode> findGraphByUserId(Long userId);

    /**
     * 删除指定笔记的所有关系
     */
    @Query("MATCH (n:SimpleNote {id: $noteId})-[r:LINKS_TO]-() DELETE r")
    void deleteRelationships(Long noteId);

    /**
     * 删除指定用户的所有节点和关系（用于重建图谱）
     */
    @Query("MATCH (n:SimpleNote) WHERE n.userId = $userId DETACH DELETE n")
    void deleteAllByUserId(Long userId);

    /**
     * 根据节点 ID 列表获取子图（仅包含这些节点之间的关系）
     */
    @Query("MATCH (n:SimpleNote) WHERE n.id IN $noteIds OPTIONAL MATCH (n)-[r:LINKS_TO|RELATED]->(m:SimpleNote) WHERE m.id IN $noteIds WITH DISTINCT n, collect(DISTINCT r) as rels, collect(DISTINCT m) as targets RETURN n, rels, targets")
    List<NoteNode> findGraphByIds(List<Long> noteIds);
}
