package com.ray.knowledgeflow.module.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户私信实体
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
@TableName("user_message")
public class UserMessage {

    @TableId
    private Long id;

    /**
     * 发送方用户ID
     */
    private Long senderId;

    /**
     * 接收方用户ID
     */
    private Long receiverId;

    /**
     * 消息正文
     */
    private String content;

    /**
     * 是否已读
     */
    private Integer isRead;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedTime;
}
