package com.ray.knowledgeflow.module.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.note.entity.NoteLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 笔记点赞 Mapper
 *
 * @author Ray
 * @since 2026-02-08
 */
@Mapper
public interface NoteLikeMapper extends BaseMapper<NoteLike> {

    /**
     * 统计笔记点赞数
     */
    @Select("SELECT COUNT(*) FROM note_like WHERE note_id = #{noteId}")
    Long countByNoteId(Long noteId);

    /**
     * 检查用户是否已点赞
     */
    @Select("SELECT COUNT(*) FROM note_like WHERE note_id = #{noteId} AND user_id = #{userId}")
    boolean existsByNoteIdAndUserId(Long noteId, Long userId);
}
