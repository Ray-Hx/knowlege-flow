package com.ray.knowledgeflow.module.spaced.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.spaced.entity.NoteReviewQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 笔记复习问题缓存 Mapper
 */
@Mapper
public interface NoteReviewQuestionMapper extends BaseMapper<NoteReviewQuestion> {
}
