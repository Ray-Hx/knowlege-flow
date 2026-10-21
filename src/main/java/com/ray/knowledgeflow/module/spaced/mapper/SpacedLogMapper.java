package com.ray.knowledgeflow.module.spaced.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.spaced.entity.SpacedLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 复习日志 Mapper 接口
 *
 * @author Ray
 * @since 2024-02-23
 */
@Mapper
public interface SpacedLogMapper extends BaseMapper<SpacedLog> {
}
