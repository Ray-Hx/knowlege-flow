package com.ray.knowledgeflow.module.ai.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.ray.knowledgeflow.module.ai.dto.ConversationDto;
import com.ray.knowledgeflow.module.ai.dto.MessageDto;
import com.ray.knowledgeflow.module.ai.dto.SendMessageDto;

import java.util.List;

/**
 * AI助手服务接口
 *
 * @author Ray
 * @since 2024-01-17
 */
public interface IAiService {

        /**
         * 创建对话
         *
         * @param userId 用户ID
         * @param title  标题
         * @return 对话详情
         */
        ConversationDto createConversation(Long userId, String title);

        /**
         * 获取对话列表
         *
         * @param userId 用户ID
         * @param page   页码
         * @param size   每页大小
         * @return 对话分页列表
         */
        IPage<ConversationDto> listConversations(Long userId, Integer page, Integer size);

        /**
         * 删除对话
         *
         * @param userId         用户ID
         * @param conversationId 对话ID
         */
        void deleteConversation(Long userId, Long conversationId);

        /**
         * 发送消息
         *
         * @param userId         用户ID
         * @param conversationId 对话ID
         * @param dto            发送消息请求
         * @return AI回复消息
         */
        MessageDto sendMessage(Long userId, Long conversationId, SendMessageDto dto);

        /**
         * 流式发送消息
         *
         * @param userId         用户ID
         * @param conversationId 对话ID
         * @param dto            发送消息请求
         * @return SseEmitter
         */
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(Long userId, Long conversationId,
                        SendMessageDto dto);

        /**
         * 获取消息历史
         *
         * @param userId         用户ID
         * @param conversationId 对话ID
         * @return 消息列表
         */
        List<MessageDto> getMessages(Long userId, Long conversationId);

        /**
         * 生成文本摘要
         *
         * @param content 原始内容
         * @return 摘要
         */
        String generateSummary(String content);

        /**
         * 提取文本标签
         *
         * @param title   标题
         * @param content 原始内容
         * @return 标签列表
         */
        List<String> extractTags(String title, String content);

        /**
         * 生成深度解析
         *
         * @param userId     当前用户ID
         * @param content    原始内容
         * @param targetType 目标类型 (NOTE, CONCEPT, SOURCE)
         * @param targetId   目标ID
         * @return 深度解析结果
         */
        com.ray.knowledgeflow.module.ai.dto.AiAnalysisResultDto generateDeepAnalysis(Long userId, String content,
                        String targetType, Long targetId);

        /**
         * 流式生成深度解析
         *
         * @param userId     当前用户ID
         * @param content    原始内容
         * @param targetType 目标类型 (NOTE, CONCEPT, SOURCE)
         * @param targetId   目标ID
         * @return SseEmitter
         */
        org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamDeepAnalysis(Long userId,
                        String content, String targetType, Long targetId);

        /**
         * 文本润色
         *
         * @param text 原始文本
         * @return 润色后的文本
         */
        String polishText(String text);

        /**
         * 文本翻译
         *
         * @param text       原始文本
         * @param targetLang 目标语言 (zh/en)
         * @return 翻译后的文本
         */
        String translateText(String text, String targetLang);

        /**
         * 文本扩写
         *
         * @param text 原始文本
         * @return 扩写后的文本
         */
        String expandText(String text);

        /**
         * 基于笔记内容生成复习问题（用于间隔重复学习）
         *
         * @param title   笔记标题
         * @param content 笔记正文（Markdown）
         * @return JSON字符串，格式: [{"question":"...","answer":"..."},...]
         */
        String generateReviewQuestions(String title, String content);
}
