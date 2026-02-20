package com.ray.knowledgeflow.module.output.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.output.dto.InviteCodeDto;
import com.ray.knowledgeflow.module.output.entity.InviteCode;
import com.ray.knowledgeflow.module.output.mapper.InviteCodeMapper;
import com.ray.knowledgeflow.module.output.service.IInviteService;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 邀请码服务实现
 *
 * @author Ray
 * @since 2024-01-26
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InviteServiceImpl implements IInviteService {

    private final InviteCodeMapper inviteCodeMapper;
    private final UserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InviteCodeDto generateCode(Long userId, Integer days) {
        InviteCode inviteCode = new InviteCode();
        inviteCode.setCode(generateUniqueCode());
        inviteCode.setCreatorId(userId);
        inviteCode.setStatus(0); // 未使用

        if (days != null && days > 0) {
            inviteCode.setExpireTime(LocalDateTime.now().plusDays(days));
        }

        inviteCodeMapper.insert(inviteCode);

        return toDto(inviteCode);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean useCode(String code, Long userId) {
        InviteCode inviteCode = inviteCodeMapper.selectOne(
                new LambdaQueryWrapper<InviteCode>()
                        .eq(InviteCode::getCode, code));

        if (inviteCode == null) {
            throw new BusinessException("邀请码不存在");
        }

        if (inviteCode.getStatus() == 1) {
            throw new BusinessException("邀请码已被使用");
        }

        if (inviteCode.getStatus() == 2) {
            throw new BusinessException("邀请码已过期");
        }

        if (inviteCode.getExpireTime() != null && inviteCode.getExpireTime().isBefore(LocalDateTime.now())) {
            inviteCode.setStatus(2);
            inviteCodeMapper.updateById(inviteCode);
            throw new BusinessException("邀请码已过期");
        }

        inviteCode.setUsedBy(userId);
        inviteCode.setUsedTime(LocalDateTime.now());
        inviteCode.setStatus(1);
        inviteCodeMapper.updateById(inviteCode);

        return true;
    }

    @Override
    public IPage<InviteCodeDto> listMyCodes(Long userId, Integer page, Integer size) {
        Page<InviteCode> pageParam = new Page<>(page, size);
        IPage<InviteCode> pageResult = inviteCodeMapper.selectPage(pageParam,
                new LambdaQueryWrapper<InviteCode>()
                        .eq(InviteCode::getCreatorId, userId)
                        .orderByDesc(InviteCode::getCreatedTime));

        return pageResult.convert(this::toDto);
    }

    @Override
    public InviteStatsDto getStats(Long userId) {
        InviteStatsDto stats = new InviteStatsDto();

        Long total = inviteCodeMapper.selectCount(
                new LambdaQueryWrapper<InviteCode>()
                        .eq(InviteCode::getCreatorId, userId));
        stats.setTotalGenerated(total.intValue());

        Long used = inviteCodeMapper.selectCount(
                new LambdaQueryWrapper<InviteCode>()
                        .eq(InviteCode::getCreatorId, userId)
                        .eq(InviteCode::getStatus, 1));
        stats.setTotalUsed(used.intValue());

        Long expired = inviteCodeMapper.selectCount(
                new LambdaQueryWrapper<InviteCode>()
                        .eq(InviteCode::getCreatorId, userId)
                        .eq(InviteCode::getStatus, 2));
        stats.setTotalExpired(expired.intValue());

        return stats;
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        } while (inviteCodeMapper.selectCount(
                new LambdaQueryWrapper<InviteCode>().eq(InviteCode::getCode, code)) > 0);
        return code;
    }

    private InviteCodeDto toDto(InviteCode inviteCode) {
        InviteCodeDto dto = new InviteCodeDto();
        BeanUtils.copyProperties(inviteCode, dto);

        if (inviteCode.getUsedBy() != null) {
            SysUser user = userMapper.selectById(inviteCode.getUsedBy());
            if (user != null) {
                dto.setUsedByUsername(user.getUsername());
            }
        }

        return dto;
    }
}
