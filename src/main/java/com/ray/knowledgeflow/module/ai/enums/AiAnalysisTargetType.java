package com.ray.knowledgeflow.module.ai.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

/**
 * AI 解析目标类型
 *
 * @author Ray
 * @since 2024-01-27
 */
public enum AiAnalysisTargetType {
    NOTE(1, "笔记"),
    CONCEPT(2, "概念"),
    SOURCE(3, "来源");

    @EnumValue
    private final int code;
    private final String desc;

    AiAnalysisTargetType(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }
}
