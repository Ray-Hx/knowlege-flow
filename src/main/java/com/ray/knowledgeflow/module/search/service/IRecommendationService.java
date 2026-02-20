package com.ray.knowledgeflow.module.search.service;

import com.ray.knowledgeflow.module.search.dto.RecommendationDto;

import java.util.List;

/**
 * 推荐服务接口
 */
public interface IRecommendationService {

    /**
     * 获取个性化推荐
     * 
     * @param userId 用户ID
     * @param limit  返回数量
     * @return 推荐列表
     */
    List<RecommendationDto> getRecommendations(Long userId, int limit);
}
