package com.ray.knowledgeflow.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.user.dto.message.MessageConversationDto;
import com.ray.knowledgeflow.module.user.dto.message.SendMessageDto;
import com.ray.knowledgeflow.module.user.dto.message.UserMessageDto;
import com.ray.knowledgeflow.module.user.entity.SysUser;
import com.ray.knowledgeflow.module.user.entity.UserMessage;
import com.ray.knowledgeflow.module.user.mapper.UserMapper;
import com.ray.knowledgeflow.module.user.mapper.UserMessageMapper;
import com.ray.knowledgeflow.module.user.service.IMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 私信服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl extends ServiceImpl<UserMessageMapper, UserMessage> implements IMessageService {

    private final UserMapper sysUserMapper;

    @Override
    public void sendMessage(Long senderId, SendMessageDto dto) {
        if (senderId.equals(dto.getReceiverId())) {
            throw new BusinessException("不能给自己发私信");
        }

        SysUser receiver = sysUserMapper.selectById(dto.getReceiverId());
        if (receiver == null) {
            throw new BusinessException("接收方不存在");
        }

        UserMessage message = new UserMessage();
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setIsRead(0);

        save(message);
    }

    @Override
    public Page<UserMessageDto> getChatHistory(Long userId1, Long userId2, int page, int size) {
        Page<UserMessage> pageParam = new Page<>(page, size);

        LambdaQueryWrapper<UserMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.nested(i -> i.eq(UserMessage::getSenderId, userId1).eq(UserMessage::getReceiverId, userId2))
                .or(i -> i.eq(UserMessage::getSenderId, userId2).eq(UserMessage::getReceiverId, userId1))
                .orderByDesc(UserMessage::getCreatedTime);

        Page<UserMessage> resultPage = page(pageParam, queryWrapper);

        SysUser user1 = sysUserMapper.selectById(userId1);
        SysUser user2 = sysUserMapper.selectById(userId2);

        Map<Long, SysUser> userMap = Map.of(
                userId1, user1 != null ? user1 : new SysUser(),
                userId2, user2 != null ? user2 : new SysUser());

        return (Page<UserMessageDto>) resultPage.convert(msg -> {
            UserMessageDto dto = new UserMessageDto();
            dto.setId(msg.getId());
            dto.setContent(msg.getContent());
            dto.setIsRead(msg.getIsRead());
            dto.setCreatedTime(msg.getCreatedTime());

            SysUser sender = userMap.get(msg.getSenderId());
            if (sender != null) {
                dto.setSenderId(sender.getId());
                dto.setSenderName(sender.getUsername());
                dto.setSenderAvatar(sender.getAvatar());
            }

            SysUser receiver = userMap.get(msg.getReceiverId());
            if (receiver != null) {
                dto.setReceiverId(receiver.getId());
                dto.setReceiverName(receiver.getUsername());
                dto.setReceiverAvatar(receiver.getAvatar());
            }

            return dto;
        });
    }

    @Override
    public List<MessageConversationDto> getConversations(Long userId) {
        return baseMapper.selectConversationSummaries(userId, 200);
    }

    @Override
    public void markAsRead(Long currentUserId, Long contactId) {
        LambdaQueryWrapper<UserMessage> updateWrapper = new LambdaQueryWrapper<>();
        updateWrapper.eq(UserMessage::getReceiverId, currentUserId)
                .eq(UserMessage::getSenderId, contactId)
                .eq(UserMessage::getIsRead, 0);

        UserMessage updateMsg = new UserMessage();
        updateMsg.setIsRead(1);

        update(updateMsg, updateWrapper);
    }

    @Override
    public long getUnreadTotal(Long userId) {
        LambdaQueryWrapper<UserMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(UserMessage::getReceiverId, userId)
                .eq(UserMessage::getIsRead, 0);
        return count(queryWrapper);
    }
}
