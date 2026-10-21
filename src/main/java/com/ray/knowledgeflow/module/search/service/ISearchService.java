package com.ray.knowledgeflow.module.search.service;

import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.note.dto.NoteListDto;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import org.springframework.data.domain.Page;

/**
 * results
 * 搜索服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface ISearchService {

        /**
         * 搜索笔记
         *
         * @param keyword 关键词
         * @param userId  用户ID
         * @param page    页码(0开始)
         * @param size    每页大小
         * @return 搜索结果
         */
        Page<NoteListDto> searchNotes(String keyword, Long userId, int page, int size);

        /**
         * 同步笔记索引
         *
         * @param note 笔记实体
         */
        void syncNote(NoteEntry note);

        /**
         * 删除笔记索引
         *
         * @param noteId 笔记ID
         */
        void deleteNoteIndex(Long noteId);

        /**
         * 同步内容索引
         * 
         * @param content 内容实体
         */
        void syncContent(ContentItem content);

        /** 删除订阅内容的 Elasticsearch 文档。 */
        void deleteContentIndex(Long contentId);

        /**
         * 搜索笔记文档（包含全文，用于RAG）
         *
         * @param keyword 关键词
         * @param userId  用户ID
         * @param size    返回数量
         * @return 笔记文档列表
         */
        java.util.List<com.ray.knowledgeflow.module.search.entity.NoteDocument> searchDocs(String keyword, Long userId,
                        int size);

        /**
         * 语义搜索聚合内容
         *
         * @param keyword 关键词
         * @param userId  用户ID
         * @param page    页码
         * @param size    每页大小
         * @return 搜索结果
         */
        Page<com.ray.knowledgeflow.module.content.dto.ContentItemDto> searchContentSemantic(String keyword, Long userId,
                        int page, int size);

        /**
         * 混合搜索聚合内容 (RRF Fusion)
         *
         * @param keyword 关键词
         * @param userId  用户ID
         * @param page    页码
         * @param size    每页大小
         * @return 搜索结果
         */
        Page<com.ray.knowledgeflow.module.content.dto.ContentItemDto> searchContentHybrid(String keyword, Long userId,
                        int page, int size);

        /**
         * 基于内容搜索相似笔记ID
         *
         * @param text  笔记文本内容
         * @param limit 返回数量
         * @return 相似笔记的ID列表
         */
        java.util.List<Long> searchSimilarNoteIds(String text, int limit);

        /**
         * 全局搜索 (同时查笔记和内容，并返回高亮)
         *
         * @param keyword 关键词
         * @param userId  用户ID
         * @return 搜索结果列表
         */
        java.util.List<com.ray.knowledgeflow.module.search.dto.SearchResultDto> globalSearch(String keyword,
                        Long userId);

        /**
         * 全量同步所有的内容到 ES
         */
        void syncAll();
}
