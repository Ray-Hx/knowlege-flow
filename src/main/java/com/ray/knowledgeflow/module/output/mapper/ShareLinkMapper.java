package com.ray.knowledgeflow.module.output.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.output.entity.ShareLink;
import org.apache.ibatis.annotations.Mapper;

/**
 * 分享链接Mapper接口
 *
 * @author Ray
 * @since 2024-01-26
 */
@Mapper
public interface ShareLinkMapper extends BaseMapper<ShareLink> {
}
