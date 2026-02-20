package com.ray.knowledgeflow.module.spaced.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 笔记复习状态实体类 (SM-2算法)
 *
 * @author Ray
 * @since 2024-01-14
 */
@Data
@TableName("spaced_review")
public class SpacedReview implements Serializable {

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
     * 笔记ID
     */
    private Long noteId;

    /**
     * 下次复习时间
     */
    private LocalDateTime nextReviewTime;

    /**
     * 间隔天数
     */
    private Integer intervalDays;

    /**
     * 重复次数
     */
    private Integer repetitionCount;

    /**
     * 难度因子 (EF)
     */
    private BigDecimal easinessFactor;

    /**
     * 上次复习时间
     */
    private LocalDateTime lastReviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
}
