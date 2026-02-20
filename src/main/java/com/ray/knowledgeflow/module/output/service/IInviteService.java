package com.ray.knowledgeflow.module.output.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.module.output.dto.InviteCodeDto;

/**
 * 邀请码服务接口
 *
 * @author Ray
 * @since 2024-01-26
 */
public interface IInviteService {

    /**
     * 生成邀请码
     *
     * @param userId 用户ID
     * @param days   有效天数（0表示永久）
     * @return 邀请码信息
     */
    InviteCodeDto generateCode(Long userId, Integer days);

    /**
     * 验证并使用邀请码
     *
     * @param code   邀请码
     * @param userId 使用者ID
     * @return 是否成功
     */
    boolean useCode(String code, Long userId);

    /**
     * 获取我的邀请码列表
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 邀请码列表
     */
    IPage<InviteCodeDto> listMyCodes(Long userId, Integer page, Integer size);

    /**
     * 获取邀请统计
     *
     * @param userId 用户ID
     * @return 统计数据
     */
    InviteStatsDto getStats(Long userId);

    /**
     * 邀请统计DTO
     */
    class InviteStatsDto {
        private Integer totalGenerated;
        private Integer totalUsed;
        private Integer totalExpired;

        public Integer getTotalGenerated() {
            return totalGenerated;
        }

        public void setTotalGenerated(Integer totalGenerated) {
            this.totalGenerated = totalGenerated;
        }

        public Integer getTotalUsed() {
            return totalUsed;
        }

        public void setTotalUsed(Integer totalUsed) {
            this.totalUsed = totalUsed;
        }

        public Integer getTotalExpired() {
            return totalExpired;
        }

        public void setTotalExpired(Integer totalExpired) {
            this.totalExpired = totalExpired;
        }
    }
}
