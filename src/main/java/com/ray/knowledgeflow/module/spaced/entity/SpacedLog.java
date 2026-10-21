package com.ray.knowledgeflow.module.spaced.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 复习日志实体类
 *
 * @author Ray
 * @since 2024-02-23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("spaced_log")
public class SpacedLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 笔记ID
     */
    private Long noteId;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 复习时间
     */
    private LocalDateTime reviewTime;

    /**
     * 质量评分 (0-5)
     */
    private Integer quality;

    /**
     * 耗时 (毫秒)
     */
    private Integer durationMs;
}
