package com.ray.knowledgeflow.module.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.module.ai.dto.ConversationDto;
import com.ray.knowledgeflow.module.ai.dto.MessageDto;
import com.ray.knowledgeflow.module.ai.dto.NoteReferenceDto;
import com.ray.knowledgeflow.module.ai.dto.SendMessageDto;
import com.ray.knowledgeflow.module.ai.entity.AiConversation;
import com.ray.knowledgeflow.module.ai.service.RagRetriever;
import com.ray.knowledgeflow.module.ai.entity.AiMessage;
import com.ray.knowledgeflow.module.ai.mapper.AiConversationMapper;
import com.ray.knowledgeflow.module.ai.mapper.AiMessageMapper;
import com.ray.knowledgeflow.module.ai.service.IAiService;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.note.mapper.NoteEntryMapper;
import com.ray.knowledgeflow.module.ai.service.ConversationMemory;
import com.ray.knowledgeflow.module.ai.service.QueryRewriter;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.StreamingResponseHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI助手服务实现类 (LangChain4j + RAG版)
 *
 * @author Ray
 * @since 2024-01-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements IAiService {

    private final AiConversationMapper conversationMapper;
    private final AiMessageMapper messageMapper;
    private final ObjectMapper objectMapper;
    private final ChatLanguageModel chatModel;
    private final StreamingChatLanguageModel streamingChatModel;
    private final EmbeddingService embeddingService;
    private final QdrantClient qdrantClient;
    private final ContentItemMapper contentItemMapper;
    private final com.ray.knowledgeflow.module.ai.mapper.AiAnalysisMapper analysisMapper;
    private final RagRetriever ragRetriever;
    private final ConversationMemory conversationMemory;
    private final QueryRewriter queryRewriter;
    private final NoteEntryMapper noteEntryMapper;
    private final ContentSourceMapper contentSourceMapper;
    @Qualifier("aiTaskExecutor")
    private final Executor aiTaskExecutor;

    @Override
    @Transactional
    public ConversationDto createConversation(Long userId, String title) {
        AiConversation conversation = new AiConversation();
        conversation.setUserId(userId);
        conversation.setTitle(StringUtils.hasText(title) ? title : "新建对话");
        conversation.setCreatedTime(LocalDateTime.now());
        conversation.setUpdatedTime(LocalDateTime.now());
        conversation.setDeleted(0);
        conversationMapper.insert(conversation);
        return toConversationDto(conversation, null);
    }

    @Override
    public IPage<ConversationDto> listConversations(Long userId, Integer page, Integer size) {
        Page<AiConversation> pageParam = new Page<>(page, size);
        IPage<AiConversation> result = conversationMapper.selectPage(pageParam,
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getUserId, userId)
                        .eq(AiConversation::getDeleted, 0)
                        .orderByDesc(AiConversation::getUpdatedTime));

        return result.convert(conv -> {
            AiMessage lastMsg = messageMapper.selectOne(
                    new LambdaQueryWrapper<AiMessage>()
                            .eq(AiMessage::getConversationId, conv.getId())
                            .orderByDesc(AiMessage::getCreatedTime)
                            .last("LIMIT 1"));
            return toConversationDto(conv, lastMsg);
        });
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        AiConversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getId, conversationId)
                        .eq(AiConversation::getUserId, userId));

        if (conv == null) {
            throw new BusinessException("对话不存在");
        }

        // 1. 使用 deleteById 触发 MyBatis-Plus 的逻辑删除 (deleted=1)
        conversationMapper.deleteById(conversationId);

        // 2. 删除关联的消息记录 (ai_message 表没有 deleted 字段，直接物理删除)
        messageMapper.delete(new LambdaQueryWrapper<AiMessage>()
                .eq(AiMessage::getConversationId, conversationId));

        log.info("用户 {} 删除了对话 {} 及其关联消息", userId, conversationId);
    }

    @Override
    @Transactional
    public MessageDto sendMessage(Long userId, Long conversationId, SendMessageDto dto) {
        // 验证对话归属
        AiConversation conv = conversationMapper.selectOne(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getId, conversationId)
                        .eq(AiConversation::getUserId, userId));
        if (conv == null) {
            throw new BusinessException("对话不存在");
        }

        // 0. 载入历史窗口。必须在保存当前消息之前，否则这一轮的提问会混进历史
        List<ChatMessage> history = conversationMemory.load(conversationId);

        // 1. 保存用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(dto.getContent());
        userMsg.setCreatedTime(LocalDateTime.now());
        messageMapper.insert(userMsg);

        // 更新对话时间
        conv.setUpdatedTime(LocalDateTime.now());
        if ("新建对话".equals(conv.getTitle()) || conv.getTitle().startsWith("新对话")) {
            conv.setTitle(
                    dto.getContent().length() > 20 ? dto.getContent().substring(0, 20) + "..." : dto.getContent());
        }
        conversationMapper.updateById(conv);

        // 2. RAG: 检索相关笔记
        List<NoteReferenceDto> references = new ArrayList<>();
        String finalPrompt = dto.getContent();

        if (Boolean.TRUE.equals(dto.getUseRag())) {
            // 检索端拿不到历史，先把追问里的指代补全再去召回
            String retrievalQuery = queryRewriter.rewrite(dto.getContent(), history);
            RagRetriever.RagContext context = ragRetriever.retrieve(
                    retrievalQuery, dto.getContent(), userId, this::resolveSubType);
            finalPrompt = context.getPrompt();
            references = context.getReferences();
        }

        // 3. 调用 AI。带上历史窗口，模型才能解析追问中的指代
        String aiResponseText;
        try {
            List<ChatMessage> messages = new ArrayList<>(history);
            messages.add(UserMessage.from(finalPrompt));
            aiResponseText = chatModel.generate(messages).content().text();
        } catch (Exception e) {
            log.error("AI调用失败", e);
            aiResponseText = "抱歉，AI服务暂时不可用，请稍后再试。";
        }

        // 4. 保存 AI 回复
        AiMessage aiMsg = new AiMessage();
        aiMsg.setConversationId(conversationId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResponseText);
        aiMsg.setReferences(referencesToJson(references));
        aiMsg.setCreatedTime(LocalDateTime.now());
        messageMapper.insert(aiMsg);

        MessageDto responseDto = toMessageDto(aiMsg);
        responseDto.setReferences(references);
        return responseDto;
    }

    @Override
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter streamChat(Long userId, Long conversationId,
            SendMessageDto dto) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

        try {
            aiTaskExecutor.execute(() -> {
            try {
                // 验证对话归属
                AiConversation conv = conversationMapper.selectOne(
                        new LambdaQueryWrapper<AiConversation>()
                                .eq(AiConversation::getId, conversationId)
                                .eq(AiConversation::getUserId, userId));
                if (conv == null) {
                    emitter.send(SseEmitter.event().name("error").data("对话不存在"));
                    emitter.complete();
                    return;
                }

                // 0. 载入历史窗口。必须在保存当前消息之前，否则这一轮的提问会混进历史
                List<ChatMessage> history = conversationMemory.load(conversationId);

                // 1. 保存用户消息
                AiMessage userMsg = new AiMessage();
                userMsg.setConversationId(conversationId);
                userMsg.setRole("user");
                userMsg.setContent(dto.getContent());
                userMsg.setCreatedTime(LocalDateTime.now());
                messageMapper.insert(userMsg);

                // 更新对话时间
                conv.setUpdatedTime(LocalDateTime.now());
                if ("新建对话".equals(conv.getTitle()) || conv.getTitle().startsWith("新对话")) {
                    conv.setTitle(
                            dto.getContent().length() > 20 ? dto.getContent().substring(0, 20) + "..."
                                    : dto.getContent());
                }
                conversationMapper.updateById(conv);

                // 发送 userMessageId 和 references 给前端的 event (可选, 为了尽早返回上下文)

                // 2. RAG: 检索相关笔记。检索端拿不到历史，先把追问里的指代补全再去召回
                RagRetriever.RagContext ragContext = Boolean.TRUE.equals(dto.getUseRag())
                        ? ragRetriever.retrieve(
                                queryRewriter.rewrite(dto.getContent(), history),
                                dto.getContent(), userId, this::resolveSubType)
                        : null;

                // 流式回调为内部类，引用的变量必须是 effectively final
                final List<NoteReferenceDto> references = ragContext != null
                        ? ragContext.getReferences()
                        : new ArrayList<>();
                final String finalPrompt = ragContext != null
                        ? ragContext.getPrompt()
                        : dto.getContent();

                // 给前端发 reference (转成json并作为引用对象给前端，这样前端可以直接渲染引用)
                try {
                    String refsJson = referencesToJson(references);
                    if (refsJson != null) {
                        emitter.send(SseEmitter.event().name("references").data(refsJson));
                    }
                } catch (Exception e) {
                    log.error("Failed to send refs", e);
                }

                // 3. 调用 AI 流式输出。带上历史窗口，模型才能解析追问中的指代
                List<ChatMessage> messages = new ArrayList<>(history);
                messages.add(UserMessage.from(finalPrompt));
                streamingChatModel.generate(messages,
                        new StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                            private final StringBuilder fullContent = new StringBuilder();

                            @Override
                            public void onNext(String token) {
                                try {
                                    emitter.send(SseEmitter.event().name("message").data(token));
                                    fullContent.append(token);
                                } catch (Exception e) {
                                    log.error("Error sending SSE chunk", e);
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                                // 4. 生成完成，保存回复
                                try {
                                    AiMessage aiMsg = new AiMessage();
                                    aiMsg.setConversationId(conversationId);
                                    aiMsg.setRole("assistant");
                                    aiMsg.setContent(fullContent.toString());
                                    aiMsg.setReferences(referencesToJson(references));
                                    aiMsg.setCreatedTime(LocalDateTime.now());
                                    messageMapper.insert(aiMsg);

                                    // 给前端发送最终消息ID（可选，让前端知道完成）
                                    emitter.send(SseEmitter.event().name("done")
                                            .data(objectMapper.writeValueAsString(toMessageDto(aiMsg))));
                                } catch (Exception e) {
                                    log.error("Error saving streamed ai message", e);
                                }
                                emitter.complete();
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("Error in streaming chat generation", error);
                                try {
                                    emitter.send(SseEmitter.event().name("error").data("生成失败"));
                                } catch (Exception ignored) {
                                }
                                emitter.completeWithError(error);
                            }
                        });

            } catch (Exception e) {
                log.error("Streaming chat setup error", e);
                emitter.completeWithError(e);
            }
            });
        } catch (RejectedExecutionException e) {
            log.warn("AI 流式任务队列已满: conversationId={}", conversationId);
            emitter.completeWithError(new BusinessException(503, "AI 服务繁忙，请稍后重试"));
        }

        return emitter;
    }

    @Override
    public List<MessageDto> getMessages(Long userId, Long conversationId) {
        // 验证归属
        Long count = conversationMapper.selectCount(
                new LambdaQueryWrapper<AiConversation>()
                        .eq(AiConversation::getId, conversationId)
                        .eq(AiConversation::getUserId, userId));
        if (count == 0) {
            throw new BusinessException("对话不存在");
        }

        List<AiMessage> messages = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByAsc(AiMessage::getCreatedTime));

        return messages.stream().map(this::toMessageDto).collect(Collectors.toList());
    }

    private String resolveSubType(Long sourceId, String sourceType) {
        if ("NOTE".equalsIgnoreCase(sourceType)) {
            return "NOTE";
        }
        // Fallback for CONTENT: check DB
        try {
            ContentItem item = contentItemMapper.selectById(sourceId);
            if (item != null) {
                return item.getSourceId() == null ? "UPLOAD" : "FEED";
            }
        } catch (Exception e) {
            log.warn("Failed to resolve subType from DB for sourceId={}", sourceId);
        }
        return "UPLOAD"; // Default to UPLOAD
    }

    // ============ 辅助方法 ============

    private ConversationDto toConversationDto(AiConversation entity, AiMessage lastMsg) {
        ConversationDto dto = new ConversationDto();
        BeanUtils.copyProperties(entity, dto);
        if (lastMsg != null) {
            dto.setLastMessage(toMessageDto(lastMsg));
        }
        return dto;
    }

    private MessageDto toMessageDto(AiMessage entity) {
        MessageDto dto = new MessageDto();
        BeanUtils.copyProperties(entity, dto);
        dto.setReferences(jsonToReferences(entity.getReferences()));
        return dto;
    }

    private String referencesToJson(List<NoteReferenceDto> refs) {
        if (refs == null || refs.isEmpty())
            return null;
        try {
            return objectMapper.writeValueAsString(refs);
        } catch (Exception e) {
            return null;
        }
    }

    private List<NoteReferenceDto> jsonToReferences(String json) {
        if (!StringUtils.hasText(json))
            return new ArrayList<>();
        try {
            return objectMapper.readValue(json, new TypeReference<List<NoteReferenceDto>>() {
            });
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public String generateSummary(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        try {
            String prompt = "请为以下内容生成一个简短的摘要（200字以内）：\n\n" +
                    (content.length() > 2000 ? content.substring(0, 2000) : content);
            return chatModel.generate(prompt).trim();
        } catch (Exception e) {
            log.error("生成摘要失败", e);
            throw new BusinessException("生成摘要失败: " + e.getMessage());
        }
    }

    @Override
    public List<String> extractTags(String title, String content) {
        // 优先使用正文，若正文为空则使用标题
        String textToAnalyze = StringUtils.hasText(content) ? content : title;

        if (!StringUtils.hasText(textToAnalyze)) {
            return new ArrayList<>();
        }
        try {
            String prompt = "你是一个专业的知识提取助手。请从以下内容中提取 3-5 个最相关的关键标签。\n" +
                    "要求：\n" +
                    "1. 只返回标签，使用英文逗号分隔。\n" +
                    "2. 不要包含任何开场白、解释或标记（如 '标签：' 或 ```）。\n" +
                    "3. 标签应为简洁的词汇（如：Java, 架构设计, 机器学习）。\n\n" +
                    "内容：\n" +
                    (textToAnalyze.length() > 2000 ? textToAnalyze.substring(0, 2000) : textToAnalyze);

            String response = chatModel.generate(prompt);
            log.info("AI 提取标签原始回复: {}", response);

            // 处理结果
            if (StringUtils.hasText(response)) {
                // 移除可能的 Markdown 代码块标记和 "标签：" 前缀
                String cleanResponse = response.replaceAll("```[a-zA-Z]*\\n?", "").replace("```", "")
                        .replaceAll("^[\\s]*标签[:：\\s]*", "").trim();

                String[] tags = cleanResponse.replace("，", ",").split(",");
                List<String> result = new ArrayList<>();
                for (String tag : tags) {
                    String cleanTag = tag.trim();
                    if (StringUtils.hasText(cleanTag)) {
                        result.add(cleanTag);
                    }
                }
                log.info("AI 提取标签解析结果: {}", result);
                return result;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("提取标签失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public com.ray.knowledgeflow.module.ai.dto.AiAnalysisResultDto generateDeepAnalysis(Long userId, String content,
            String targetTypeStr, Long targetId) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType targetType =
                resolveOwnedAnalysisTarget(userId, targetTypeStr, targetId);

        try {
            // Check cache
            if (targetType != null && targetId != null) {
                com.ray.knowledgeflow.module.ai.entity.AiAnalysis existing = analysisMapper.selectOne(
                        new LambdaQueryWrapper<com.ray.knowledgeflow.module.ai.entity.AiAnalysis>()
                                .eq(com.ray.knowledgeflow.module.ai.entity.AiAnalysis::getTargetType, targetType)
                                .eq(com.ray.knowledgeflow.module.ai.entity.AiAnalysis::getTargetId, targetId));

                if (existing != null) {
                    return new com.ray.knowledgeflow.module.ai.dto.AiAnalysisResultDto(existing.getContent(),
                            existing.getUpdatedTime());
                }
            }

            // Generate
            // 构建深度解析 Prompt
            String prompt = "请对以下内容进行深度解析。你的回答应包含以下 Markdown 部分：\n" +
                    "### 核心概念\n" +
                    "- 简要列出内容中的关键概念及其定义\n\n" +
                    "### 关键观点\n" +
                    "- 总结作者的主要论点或核心思想\n\n" +
                    "### 深度分析\n" +
                    "- 分析内容的逻辑结构、优缺点，以及潜在的深层含义\n\n" +
                    "### 启示与应用\n" +
                    "- 这些内容对实际工作或学习有什么启示？\n\n" +
                    "以下是待解析内容（截取前3000字）：\n\n" +
                    (content.length() > 3000 ? content.substring(0, 3000) : content);

            String result = chatModel.generate(prompt);

            // Save
            if (targetType != null && targetId != null && StringUtils.hasText(result)) {
                com.ray.knowledgeflow.module.ai.entity.AiAnalysis analysis = new com.ray.knowledgeflow.module.ai.entity.AiAnalysis();
                analysis.setTargetType(targetType);
                analysis.setTargetId(targetId);
                analysis.setContent(result);
                analysis.setCreatedTime(LocalDateTime.now());
                analysis.setUpdatedTime(LocalDateTime.now());
                analysisMapper.insert(analysis);
                return new com.ray.knowledgeflow.module.ai.dto.AiAnalysisResultDto(result, analysis.getUpdatedTime());
            }

            return new com.ray.knowledgeflow.module.ai.dto.AiAnalysisResultDto(result, LocalDateTime.now());

        } catch (Exception e) {
            log.error("深度解析失败", e);
            throw new BusinessException("深度解析失败: " + e.getMessage());
        }
    }

    @Override
    public SseEmitter streamDeepAnalysis(Long userId, String content, String targetTypeStr, Long targetId) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

        if (!StringUtils.hasText(content)) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // ignore
            }
            return emitter;
        }

        // 在开始 SSE 响应前完成权限校验，避免后台线程读取其他用户的缓存。
        final com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType targetType =
                resolveOwnedAnalysisTarget(userId, targetTypeStr, targetId);

        try {
            aiTaskExecutor.execute(() -> {
            try {
                // Check cache
                if (targetType != null && targetId != null) {
                    com.ray.knowledgeflow.module.ai.entity.AiAnalysis existing = analysisMapper.selectOne(
                            new LambdaQueryWrapper<com.ray.knowledgeflow.module.ai.entity.AiAnalysis>()
                                    .eq(com.ray.knowledgeflow.module.ai.entity.AiAnalysis::getTargetType, targetType)
                                    .eq(com.ray.knowledgeflow.module.ai.entity.AiAnalysis::getTargetId, targetId));

                    if (existing != null) {
                        // Cache hit: Stream the existing content
                        String cachedContent = existing.getContent();
                        // Simulate streaming for better UX or just send at once?
                        // For now, let's send larger chunks to be faster but still look like stream
                        int chunkSize = 50;
                        for (int i = 0; i < cachedContent.length(); i += chunkSize) {
                            int end = Math.min(cachedContent.length(), i + chunkSize);
                            emitter.send(SseEmitter.event().data(cachedContent.substring(i, end)));
                            Thread.sleep(10); // slightly delay
                        }
                        emitter.complete();
                        return;
                    }
                }

                // Cache miss: Generate via LLM
                String prompt = "请对以下内容进行深度解析。你的回答应包含以下 Markdown 部分：\n" +
                        "### 核心概念\n" +
                        "- 简要列出内容中的关键概念及其定义\n\n" +
                        "### 关键观点\n" +
                        "- 总结作者的主要论点或核心思想\n\n" +
                        "### 深度分析\n" +
                        "- 分析内容的逻辑结构、优缺点，以及潜在的深层含义\n\n" +
                        "### 启示与应用\n" +
                        "- 这些内容对实际工作或学习有什么启示？\n\n" +
                        "以下是待解析内容（截取前3000字）：\n\n" +
                        (content.length() > 3000 ? content.substring(0, 3000) : content);

                final com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType finalTargetType = targetType;

                streamingChatModel.generate(prompt,
                        new StreamingResponseHandler<dev.langchain4j.data.message.AiMessage>() {
                            private final StringBuilder fullContent = new StringBuilder();

                            @Override
                            public void onNext(String token) {
                                try {
                                    emitter.send(SseEmitter.event().data(token));
                                    fullContent.append(token);
                                } catch (Exception e) {
                                    log.error("Error sending SSE", e);
                                    emitter.completeWithError(e);
                                }
                            }

                            @Override
                            public void onComplete(Response<dev.langchain4j.data.message.AiMessage> response) {
                                // Save to DB
                                if (finalTargetType != null && targetId != null && fullContent.length() > 0) {
                                    try {
                                        com.ray.knowledgeflow.module.ai.entity.AiAnalysis analysis = new com.ray.knowledgeflow.module.ai.entity.AiAnalysis();
                                        analysis.setTargetType(finalTargetType);
                                        analysis.setTargetId(targetId);
                                        analysis.setContent(fullContent.toString());
                                        analysis.setCreatedTime(LocalDateTime.now());
                                        analysis.setUpdatedTime(LocalDateTime.now());
                                        analysisMapper.insert(analysis);
                                    } catch (Exception e) {
                                        log.error("Error saving analysis to DB", e);
                                    }
                                }
                                emitter.complete();
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("Error in streaming generation", error);
                                emitter.completeWithError(error);
                            }
                        });

            } catch (Exception e) {
                log.error("Streaming error", e);
                emitter.completeWithError(e);
            }
            });
        } catch (RejectedExecutionException e) {
            log.warn("AI 深度解析任务队列已满: targetType={}, targetId={}", targetTypeStr, targetId);
            emitter.completeWithError(new BusinessException(503, "AI 服务繁忙，请稍后重试"));
        }

        return emitter;
    }

    /**
     * 只有能证明属于当前用户的持久化目标才允许读写全局解析缓存。
     * CONCEPT 当前没有用户归属事实表，因此仍可解析客户端提供的文本，但禁用缓存。
     */
    private com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType resolveOwnedAnalysisTarget(
            Long userId, String targetTypeStr, Long targetId) {
        if (!StringUtils.hasText(targetTypeStr) || targetId == null) {
            return null;
        }

        final com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType targetType;
        try {
            targetType = com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType
                    .valueOf(targetTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(400, "目标类型无效");
        }

        if (targetType == com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType.CONCEPT) {
            log.debug("概念解析暂无用户归属事实表，本次不使用持久化缓存: targetId={}", targetId);
            return null;
        }

        boolean owned = switch (targetType) {
            case NOTE -> noteEntryMapper.selectCount(
                    new LambdaQueryWrapper<com.ray.knowledgeflow.module.note.entity.NoteEntry>()
                            .eq(com.ray.knowledgeflow.module.note.entity.NoteEntry::getId, targetId)
                            .eq(com.ray.knowledgeflow.module.note.entity.NoteEntry::getUserId, userId)) > 0;
            case SOURCE -> contentSourceMapper.selectCount(
                    new LambdaQueryWrapper<com.ray.knowledgeflow.module.content.entity.ContentSource>()
                            .eq(com.ray.knowledgeflow.module.content.entity.ContentSource::getId, targetId)
                            .eq(com.ray.knowledgeflow.module.content.entity.ContentSource::getUserId, userId)) > 0;
            case CONCEPT -> false;
        };

        if (!owned) {
            throw new BusinessException(404, "解析目标不存在");
        }
        return targetType;
    }

    @Override
    public String polishText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        try {
            String prompt = "请对以下文本进行润色，使其更加流畅、优雅、专业。保持原意，只返回润色后的文本，不要包含任何解释：\n\n" + text;
            return chatModel.generate(prompt).trim();
        } catch (Exception e) {
            log.error("文本润色失败", e);
            throw new BusinessException("文本润色失败: " + e.getMessage());
        }
    }

    @Override
    public String translateText(String text, String targetLang) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        try {
            String langName = "en".equalsIgnoreCase(targetLang) ? "English" : "中文";
            String prompt = "请将以下文本翻译成" + langName + "。只返回翻译结果，不要包含任何解释：\n\n" + text;
            return chatModel.generate(prompt).trim();
        } catch (Exception e) {
            log.error("文本翻译失败", e);
            throw new BusinessException("文本翻译失败: " + e.getMessage());
        }
    }

    @Override
    public String expandText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        try {
            String prompt = "请将以下文本进行扩写，增加更多细节、例子或解释，使内容更丰富。保持原有风格，只返回扩写后的文本：\n\n" + text;
            return chatModel.generate(prompt).trim();
        } catch (Exception e) {
            log.error("文本扩写失败", e);
            throw new BusinessException("文本扩写失败: " + e.getMessage());
        }
    }

    @Override
    public String generateReviewQuestions(String title, String content) {
        // 优先使用正文，若正文为空则使用标题作为提示
        String mainContent = StringUtils.hasText(content) ? content : "";
        if (mainContent.length() > 3000) {
            mainContent = mainContent.substring(0, 3000);
        }

        if (!StringUtils.hasText(mainContent)) {
            // 内容为空，无法出有意义的题，返回空数组
            return "[]";
        }

        String prompt = "你是一位专业的知识测验出题助手。请仔细阅读以下笔记内容，然后根据其中的核心知识点、概念、原理或重要细节，生成 3 道高质量的复习问答题。\n\n" +
                "要求：\n" +
                "1. 问题必须基于笔记正文中的实际内容，不能问笔记的标题、作者、格式等元信息\n" +
                "2. 问题应考查对知识点的理解，而非死记硬背\n" +
                "3. 参考答案应简洁、准确，控制在 100 字以内\n" +
                "4. 只返回 JSON 数组，不要包含任何其他文字或代码块标记\n" +
                "5. 格式严格如下：\n" +
                "[{\"question\":\"问题1\",\"answer\":\"答案1\"},{\"question\":\"问题2\",\"answer\":\"答案2\"},{\"question\":\"问题3\",\"answer\":\"答案3\"}]\n\n"
                +
                "笔记标题：" + title + "\n\n" +
                "笔记正文：\n" + mainContent;

        try {
            return chatModel.generate(prompt).trim();
        } catch (Exception e) {
            log.error("生成复习问题失败", e);
            return "[]";
        }
    }
}
