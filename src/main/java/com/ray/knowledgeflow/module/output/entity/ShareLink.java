package com.ray.knowledgeflow.module.output.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 分享链接实体类
 *
 * @author Ray
 * @since 2024-01-26
 */
@Data
@TableName("share_link")
public class ShareLink implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 资源类型：note, folder, collection
     */
    private String resourceType;

    /**
     * 资源ID
     */
    private Long resourceId;

    /**
     * 分享码
     */
    private String shareCode;

    /**
     * 访问密码
     */
    private String password;

    /**
     * 过期时间
     */
    private LocalDateTime expireTime;

    /**
     * 查看次数
     */
    private Integer viewCount;

    /**
     * 状态：0-禁用，1-启用
     */
    private Integer status;

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

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer deleted;
}
