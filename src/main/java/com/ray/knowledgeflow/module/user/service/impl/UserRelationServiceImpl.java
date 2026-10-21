package com.ray.knowledgeflow.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.user.entity.UserRelation;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import com.ray.knowledgeflow.module.user.mapper.UserRelationMapper;
import com.ray.knowledgeflow.module.user.service.IUserRelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 用户关系服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Service
@RequiredArgsConstructor
public class UserRelationServiceImpl extends ServiceImpl<UserRelationMapper, UserRelation>
        implements IUserRelationService {

    private final UserMapper userMapper;

    @Override
    public void follow(Long followerId, Long followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException(400, "不能关注自己");
        }

        if (userMapper.selectById(followingId) == null) {
            throw new BusinessException(404, "用户不存在");
        }

        if (isFollowing(followerId, followingId)) {
            return; // 已经关注
        }

        UserRelation relation = new UserRelation();
        relation.setFollowerId(followerId);
        relation.setFollowingId(followingId);
        relation.setCreatedTime(LocalDateTime.now());

        this.save(relation);
    }

    @Override
    public void unfollow(Long followerId, Long followingId) {
        LambdaQueryWrapper<UserRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelation::getFollowerId, followerId)
                .eq(UserRelation::getFollowingId, followingId);

        this.remove(wrapper);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        if (followerId == null || followingId == null)
            return false;

        LambdaQueryWrapper<UserRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelation::getFollowerId, followerId)
                .eq(UserRelation::getFollowingId, followingId);

        return this.count(wrapper) > 0;
    }

    @Override
    public long getFollowerCount(Long userId) {
        LambdaQueryWrapper<UserRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelation::getFollowingId, userId);
        return this.count(wrapper);
    }

    @Override
    public long getFollowingCount(Long userId) {
        LambdaQueryWrapper<UserRelation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserRelation::getFollowerId, userId);
        return this.count(wrapper);
    }
}
