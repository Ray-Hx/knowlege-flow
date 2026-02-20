package com.ray.knowledgeflow.module.output.service;

import com.ray.knowledgeflow.module.output.dto.VisitorStatsDto;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 访客服务接口
 *
 * @author Ray
 * @since 2024-01-26
 */
public interface IVisitorService {

    /**
     * 记录访问
     *
     * @param userId       被访问用户ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param request      HTTP请求
     */
    void recordVisit(Long userId, String resourceType, Long resourceId, HttpServletRequest request);

    /**
     * 获取访客统计
     *
     * @param userId 用户ID
     * @return 统计数据
     */
    VisitorStatsDto getStats(Long userId);
}
