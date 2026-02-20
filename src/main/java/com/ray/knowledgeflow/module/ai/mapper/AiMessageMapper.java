package com.ray.knowledgeflow.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.ai.entity.AiMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI消息Mapper接口
 *
 * @author Ray
 * @since 2024-01-17
 */
@Mapper
public interface AiMessageMapper extends BaseMapper<AiMessage> {
}
