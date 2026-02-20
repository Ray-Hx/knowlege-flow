package com.ray.knowledgeflow.module.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import org.apache.ibatis.annotations.Mapper;

/**
 * 内容项 Mapper
 *
 * @author Ray
 * @since 2024-01-16
 */
@Mapper
public interface ContentItemMapper extends BaseMapper<ContentItem> {
}
