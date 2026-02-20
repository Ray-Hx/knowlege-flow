package com.ray.knowledgeflow.module.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.note.entity.NoteComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记评论 Mapper
 *
 * @author Ray
 * @since 2026-02-20
 */
@Mapper
public interface NoteCommentMapper extends BaseMapper<NoteComment> {
}
