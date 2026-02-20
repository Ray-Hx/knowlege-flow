package com.ray.knowledgeflow.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.ai.entity.AiConversation;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI对话Mapper接口
 *
 * @author Ray
 * @since 2024-01-17
 */
@Mapper
public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
