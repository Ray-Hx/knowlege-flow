package com.ray.knowledgeflow.module.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 用户扩展信息实体类
 * </p>
 *
 * @author Ray
 * @since 2024-01-14
 */
@Data
@TableName("sys_user_profile")
public class SysUserProfile implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID (主键 & 外键)
     */
    @TableId(value = "user_id")
    private Long userId;

    /**
     * 个人简介
     */
    private String bio;

    /**
     * GitHub ID
     */
    private String githubId;

    /**
     * GitHub Personal Access Token (加密)
     * (Temporarily excluded from DB mapping as column is missing)
     */
    @TableField(exist = false)
    private String githubToken;

    /**
     * 用户偏好设置 (JSON)
     */
    private String preferences;

    /**
     * 学习目标 (JSON)
     */
    private String learningGoals;

    /**
     * 是否公开个人主页: 0-否, 1-是
     */
    private Integer isPublic;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
