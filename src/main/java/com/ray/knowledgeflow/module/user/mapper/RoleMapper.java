package com.ray.knowledgeflow.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.user.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 角色 Mapper 接口
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
@Mapper
public interface RoleMapper extends BaseMapper<SysRole> {
}
