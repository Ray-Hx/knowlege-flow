package com.ray.knowledgeflow.module.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ray.knowledgeflow.module.user.entity.UserRelation;

/**
 * 用户关系服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IUserRelationService extends IService<UserRelation> {

    /**
     * 关注用户
     *
     * @param followerId  当前用户（粉丝）
     * @param followingId 被关注用户（博主）
     */
    void follow(Long followerId, Long followingId);

    /**
     * 取消关注
     *
     * @param followerId  当前用户（粉丝）
     * @param followingId 被关注用户（博主）
     */
    void unfollow(Long followerId, Long followingId);

    /**
     * 检查是否已关注
     *
     * @param followerId  当前用户（粉丝）
     * @param followingId 被关注用户（博主）
     * @return true 如果已经关注
     */
    boolean isFollowing(Long followerId, Long followingId);

    /**
     * 获取粉丝数量
     */
    long getFollowerCount(Long userId);

    /**
     * 获取关注数量
     */
    long getFollowingCount(Long userId);
}
