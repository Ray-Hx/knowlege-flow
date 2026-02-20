package com.ray.knowledgeflow.module.search.repository;

import com.ray.knowledgeflow.module.search.entity.NoteDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 笔记搜索Repository
 *
 * @author Ray
 * @since 2024-01-16
 */
@Repository
public interface NoteRepository extends ElasticsearchRepository<NoteDocument, Long> {

    Page<NoteDocument> findByUserId(Long userId, Pageable pageable);
}
