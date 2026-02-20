package com.ray.knowledgeflow.module.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 每日学习数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyStudyDto {

    /**
     * 日期 (yyyy-MM-dd)
     */
    private String date;

    /**
     * 学习时长 (分钟)
     */
    private Integer studyMinutes;

    /**
     * 笔记数量
     */
    private Integer noteCount;

    /**
     * 完成的番茄钟数
     */
    private Integer pomodoroCount;
}
