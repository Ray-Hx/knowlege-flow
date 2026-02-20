package com.ray.knowledgeflow.module.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户扩展信息 Mapper 接口
 * </p>
 *
 * @author Ray
 * @since 2024-01-16
 */
@Mapper
public interface UserProfileMapper extends BaseMapper<SysUserProfile> {
}
