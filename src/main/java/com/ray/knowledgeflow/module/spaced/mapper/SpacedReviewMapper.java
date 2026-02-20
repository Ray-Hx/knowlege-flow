package com.ray.knowledgeflow.module.spaced.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.spaced.entity.SpacedReview;
import org.apache.ibatis.annotations.Mapper;

/**
 * 复习状态 Mapper 接口
 *
 * @author Ray
 * @since 2024-01-17
 */
@Mapper
public interface SpacedReviewMapper extends BaseMapper<SpacedReview> {
}
