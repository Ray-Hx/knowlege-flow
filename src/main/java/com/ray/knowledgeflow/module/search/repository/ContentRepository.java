package com.ray.knowledgeflow.module.search.repository;

import com.ray.knowledgeflow.module.search.entity.ContentDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * 聚合内容搜索Repository
 *
 * @author Ray
 * @since 2024-01-16
 */
@Repository
public interface ContentRepository extends ElasticsearchRepository<ContentDocument, Long> {

    Page<ContentDocument> findByUserId(Long userId, Pageable pageable);
}
