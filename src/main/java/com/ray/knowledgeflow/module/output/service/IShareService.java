package com.ray.knowledgeflow.module.output.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.module.output.dto.ShareLinkDto;

/**
 * 分享服务接口
 *
 * @author Ray
 * @since 2024-01-26
 */
public interface IShareService {

    /**
     * 创建分享链接
     *
     * @param userId       用户ID
     * @param resourceType 资源类型
     * @param resourceId   资源ID
     * @param password     访问密码（可选）
     * @param days         有效天数（0表示永久）
     * @return 分享链接信息
     */
    ShareLinkDto createShareLink(Long userId, String resourceType, Long resourceId, String password, Integer days);

    /**
     * 获取分享内容
     *
     * @param shareCode 分享码
     * @param password  密码（如果需要）
     * @return 分享内容
     */
    Object getSharedContent(String shareCode, String password);

    /**
     * 获取我的分享列表
     *
     * @param userId 用户ID
     * @param page   页码
     * @param size   每页大小
     * @return 分享列表
     */
    IPage<ShareLinkDto> listMyShares(Long userId, Integer page, Integer size);

    /**
     * 删除分享链接
     *
     * @param userId    用户ID
     * @param shareCode 分享码
     */
    void deleteShare(Long userId, String shareCode);

    /**
     * 更新分享状态
     *
     * @param userId    用户ID
     * @param shareCode 分享码
     * @param status    状态
     */
    void updateStatus(Long userId, String shareCode, Integer status);
}
