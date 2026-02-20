package com.ray.knowledgeflow.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户关注关系实体类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
@TableName("user_relation")
public class UserRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关注者ID（粉丝）
     */
    private Long followerId;

    /**
     * 被关注者ID（博主）
     */
    private Long followingId;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

}
