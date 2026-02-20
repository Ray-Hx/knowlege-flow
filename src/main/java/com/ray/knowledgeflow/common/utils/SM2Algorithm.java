package com.ray.knowledgeflow.common.utils;

import lombok.Data;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * SM-2 间隔重复算法工具类
 * 
 * <p>
 * SuperMemo-2 算法用于计算最优复习间隔，基于遗忘曲线理论。
 * 算法根据用户的回忆质量评分（0-5）动态调整复习间隔和难度因子。
 * </p>
 *
 * <p>
 * 算法规则：
 * </p>
 * <ul>
 * <li>质量评分 q: 0-5，5表示完美记忆，0表示完全遗忘</li>
 * <li>难度因子 EF: 最小值1.3，初始值2.5</li>
 * <li>间隔计算：I(1)=1天，I(2)=6天，I(n)=I(n-1)*EF</li>
 * <li>评分<3时重置学习进度</li>
 * </ul>
 *
 * @author Ray
 * @since 2024-01-17
 */
public class SM2Algorithm {

    /**
     * 最小难度因子
     */
    private static final BigDecimal MIN_EF = new BigDecimal("1.3");

    /**
     * 初始难度因子
     */
    private static final BigDecimal INITIAL_EF = new BigDecimal("2.5");

    /**
     * 第一次复习间隔（天）
     */
    private static final int FIRST_INTERVAL = 1;

    /**
     * 第二次复习间隔（天）
     */
    private static final int SECOND_INTERVAL = 6;

    /**
     * 计算下次复习参数
     *
     * @param quality         质量评分 (0-5)
     * @param repetitionCount 当前重复次数
     * @param intervalDays    当前间隔天数
     * @param easinessFactor  当前难度因子
     * @return SM2计算结果
     */
    public static SM2Result calculate(int quality, int repetitionCount,
            int intervalDays, BigDecimal easinessFactor) {
        // 验证输入参数
        if (quality < 0 || quality > 5) {
            throw new IllegalArgumentException("质量评分必须在0-5之间");
        }

        SM2Result result = new SM2Result();

        // 计算新的难度因子 (EF)
        // EF' = EF + (0.1 - (5-q)*(0.08+(5-q)*0.02))
        BigDecimal newEF = calculateEasinessFactor(quality, easinessFactor);
        result.setNewEasinessFactor(newEF);

        // 质量评分 < 3 时，重置学习进度
        if (quality < 3) {
            result.setNewRepetitionCount(0);
            result.setNewIntervalDays(FIRST_INTERVAL);
        } else {
            // 增加重复次数
            int newRepetitionCount = repetitionCount + 1;
            result.setNewRepetitionCount(newRepetitionCount);

            // 计算新的间隔
            int newIntervalDays = calculateInterval(newRepetitionCount, intervalDays, newEF);
            result.setNewIntervalDays(newIntervalDays);
        }

        // 计算下次复习时间
        result.setNextReviewTime(LocalDateTime.now().plusDays(result.getNewIntervalDays()));

        return result;
    }

    /**
     * 计算难度因子
     *
     * @param quality   质量评分
     * @param currentEF 当前难度因子
     * @return 新的难度因子
     */
    private static BigDecimal calculateEasinessFactor(int quality, BigDecimal currentEF) {
        // EF' = EF + (0.1 - (5-q)*(0.08+(5-q)*0.02))
        int diff = 5 - quality;

        BigDecimal factor1 = new BigDecimal("0.08");
        BigDecimal factor2 = new BigDecimal("0.02").multiply(new BigDecimal(diff));
        BigDecimal factor3 = factor1.add(factor2).multiply(new BigDecimal(diff));
        BigDecimal adjustment = new BigDecimal("0.1").subtract(factor3);

        BigDecimal newEF = currentEF.add(adjustment);

        // 确保 EF >= 1.3
        if (newEF.compareTo(MIN_EF) < 0) {
            newEF = MIN_EF;
        }

        return newEF.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算复习间隔
     *
     * @param repetitionCount 重复次数
     * @param currentInterval 当前间隔
     * @param easinessFactor  难度因子
     * @return 新的间隔天数
     */
    private static int calculateInterval(int repetitionCount, int currentInterval,
            BigDecimal easinessFactor) {
        if (repetitionCount == 1) {
            return FIRST_INTERVAL;
        } else if (repetitionCount == 2) {
            return SECOND_INTERVAL;
        } else {
            // I(n) = I(n-1) * EF
            BigDecimal newInterval = new BigDecimal(currentInterval).multiply(easinessFactor);
            return newInterval.setScale(0, RoundingMode.HALF_UP).intValue();
        }
    }

    /**
     * 获取初始复习状态
     *
     * @return 初始SM2结果
     */
    public static SM2Result getInitialState() {
        SM2Result result = new SM2Result();
        result.setNewRepetitionCount(0);
        result.setNewIntervalDays(0);
        result.setNewEasinessFactor(INITIAL_EF);
        result.setNextReviewTime(LocalDateTime.now());
        return result;
    }

    /**
     * SM-2 算法计算结果
     */
    @Data
    public static class SM2Result {
        /**
         * 新的重复次数
         */
        private int newRepetitionCount;

        /**
         * 新的间隔天数
         */
        private int newIntervalDays;

        /**
         * 新的难度因子
         */
        private BigDecimal newEasinessFactor;

        /**
         * 下次复习时间
         */
        private LocalDateTime nextReviewTime;
    }
}
