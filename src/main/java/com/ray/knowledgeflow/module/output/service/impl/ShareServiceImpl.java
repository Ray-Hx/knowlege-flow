package com.ray.knowledgeflow.module.output.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.note.entity.NoteEntry;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.output.dto.ShareLinkDto;
import com.ray.knowledgeflow.module.output.entity.ShareLink;
import com.ray.knowledgeflow.module.output.mapper.ShareLinkMapper;
import com.ray.knowledgeflow.module.output.service.IShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 分享服务实现
 *
 * @author Ray
 * @since 2024-01-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShareServiceImpl implements IShareService {

    private final ShareLinkMapper shareLinkMapper;
    private final NoteEntryMapper noteEntryMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ShareLinkDto createShareLink(Long userId, String resourceType, Long resourceId, String password,
            Integer days) {
        ShareLink shareLink = new ShareLink();
        shareLink.setUserId(userId);
        shareLink.setResourceType(resourceType);
        shareLink.setResourceId(resourceId);
        shareLink.setShareCode(generateUniqueCode());
        shareLink.setPassword(password);
        shareLink.setViewCount(0);
        shareLink.setStatus(1);

        if (days != null && days > 0) {
            shareLink.setExpireTime(LocalDateTime.now().plusDays(days));
        }

        shareLinkMapper.insert(shareLink);

        return toDto(shareLink);
    }

    @Override
    public Object getSharedContent(String shareCode, String password) {
        ShareLink shareLink = shareLinkMapper.selectOne(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getShareCode, shareCode));

        if (shareLink == null) {
            throw new BusinessException("分享不存在");
        }

        if (shareLink.getStatus() == 0) {
            throw new BusinessException("分享已禁用");
        }

        if (shareLink.getExpireTime() != null && shareLink.getExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException("分享已过期");
        }

        if (shareLink.getPassword() != null && !shareLink.getPassword().equals(password)) {
            throw new BusinessException("密码错误");
        }

        // 增加查看次数
        shareLink.setViewCount(shareLink.getViewCount() + 1);
        shareLinkMapper.updateById(shareLink);

        // 根据资源类型返回内容
        if ("note".equals(shareLink.getResourceType())) {
            NoteEntry note = noteEntryMapper.selectById(shareLink.getResourceId());
            if (note == null || note.getIsPublic() == 0) {
                throw new BusinessException("笔记不存在或未公开");
            }
            return note;
        }

        // TODO: 处理其他资源类型（folder, collection等）
        throw new BusinessException("不支持的资源类型");
    }

    @Override
    public IPage<ShareLinkDto> listMyShares(Long userId, Integer page, Integer size) {
        Page<ShareLink> pageParam = new Page<>(page, size);
        IPage<ShareLink> pageResult = shareLinkMapper.selectPage(pageParam,
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getUserId, userId)
                        .orderByDesc(ShareLink::getCreatedTime));

        return pageResult.convert(this::toDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteShare(Long userId, String shareCode) {
        ShareLink shareLink = shareLinkMapper.selectOne(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getShareCode, shareCode)
                        .eq(ShareLink::getUserId, userId));

        if (shareLink == null) {
            throw new BusinessException("分享不存在");
        }

        shareLinkMapper.deleteById(shareLink.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long userId, String shareCode, Integer status) {
        ShareLink shareLink = shareLinkMapper.selectOne(
                new LambdaQueryWrapper<ShareLink>()
                        .eq(ShareLink::getShareCode, shareCode)
                        .eq(ShareLink::getUserId, userId));

        if (shareLink == null) {
            throw new BusinessException("分享不存在");
        }

        shareLink.setStatus(status);
        shareLinkMapper.updateById(shareLink);
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        } while (shareLinkMapper.selectCount(
                new LambdaQueryWrapper<ShareLink>().eq(ShareLink::getShareCode, code)) > 0);
        return code;
    }

    private ShareLinkDto toDto(ShareLink shareLink) {
        ShareLinkDto dto = new ShareLinkDto();
        BeanUtils.copyProperties(shareLink, dto);
        dto.setHasPassword(shareLink.getPassword() != null && !shareLink.getPassword().isEmpty());

        // 获取资源标题
        if ("note".equals(shareLink.getResourceType())) {
            NoteEntry note = noteEntryMapper.selectById(shareLink.getResourceId());
            if (note != null) {
                dto.setResourceTitle(note.getTitle());
            }
        }

        return dto;
    }
}
