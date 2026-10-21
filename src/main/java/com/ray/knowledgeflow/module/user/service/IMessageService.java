package com.ray.knowledgeflow.module.user.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ray.knowledgeflow.module.user.dto.message.MessageConversationDto;
import com.ray.knowledgeflow.module.user.dto.message.SendMessageDto;
import com.ray.knowledgeflow.module.user.dto.message.UserMessageDto;

import java.util.List;

/**
 * 消息服务接口
 */
public interface IMessageService {

    /**
     * 发送私信
     */
    void sendMessage(Long senderId, SendMessageDto dto);

    /**
     * 获取两个用户之间的聊天记录
     */
    Page<UserMessageDto> getChatHistory(Long userId1, Long userId2, int page, int size);

    /**
     * 获取当前用户的会话列表
     */
    List<MessageConversationDto> getConversations(Long userId);

    /**
     * 将某个联系人的消息标记为已读
     */
    void markAsRead(Long currentUserId, Long contactId);

    /**
     * 获取未读消息总数
     */
    long getUnreadTotal(Long userId);
}
