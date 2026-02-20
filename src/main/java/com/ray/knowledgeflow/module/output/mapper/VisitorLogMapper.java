package com.ray.knowledgeflow.module.output.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ray.knowledgeflow.module.output.entity.VisitorLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

/**
 * 访客记录Mapper接口
 *
 * @author Ray
 * @since 2024-01-26
 */
@Mapper
public interface VisitorLogMapper extends BaseMapper<VisitorLog> {

    /**
     * 统计指定时间范围内的访问量
     */
    @Select("SELECT COUNT(*) FROM visitor_log WHERE user_id = #{userId} AND visit_time >= #{startTime}")
    Integer countVisitsSince(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);

    /**
     * 统计指定时间范围内的独立访客数（按IP去重）
     */
    @Select("SELECT COUNT(DISTINCT visitor_ip) FROM visitor_log WHERE user_id = #{userId} AND visit_time >= #{startTime}")
    Integer countUniqueVisitorsSince(@Param("userId") Long userId, @Param("startTime") LocalDateTime startTime);
}
