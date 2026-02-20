package com.ray.knowledgeflow.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.user.entity.UserRelation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户关注关系 Mapper 接口
 *
 * @author Ray
 * @since 2024-01-16
 */
@Mapper
public interface UserRelationMapper extends BaseMapper<UserRelation> {
}
