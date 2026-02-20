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
import com.ray.knowledgeflow.module.ai.entity.AiMessage;
import com.ray.knowledgeflow.module.ai.mapper.AiConversationMapper;
import com.ray.knowledgeflow.module.ai.mapper.AiMessageMapper;
import com.ray.knowledgeflow.module.ai.service.IAiService;
import com.ray.knowledgeflow.module.search.service.EmbeddingService;
import dev.langchain4j.model.chat.ChatLanguageModel;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.model.StreamingResponseHandler;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    private final com.ray.knowledgeflow.module.ai.mapper.AiAnalysisMapper analysisMapper;

    private static final String CHUNK_COLLECTION = "content_chunks";

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

        // 逻辑删除
        conv.setDeleted(1);
        conversationMapper.updateById(conv);
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
            try {
                // 使用 Qdrant 向量检索 Content Chunks
                log.info("RAG检索开始: query={}, userId={}", dto.getContent(), userId);
                List<Float> vector = embeddingService.embed(dto.getContent());
                log.info("向量生成完成: vectorSize={}", vector.size());

                if (!vector.isEmpty()) {
                    // 构建用户 ID 过滤条件 - 只检索当前用户的笔记
                    Points.Filter userFilter = Points.Filter.newBuilder()
                            .addMust(Points.Condition.newBuilder()
                                    .setField(Points.FieldCondition.newBuilder()
                                            .setKey("userId")
                                            .setMatch(Points.Match.newBuilder()
                                                    .setInteger(userId)
                                                    .build())
                                            .build())
                                    .build())
                            .build();

                    Points.SearchPoints searchPoints = Points.SearchPoints.newBuilder()
                            .setCollectionName(CHUNK_COLLECTION)
                            .addAllVector(vector)
                            .setLimit(5)
                            .setFilter(userFilter) // 按用户 ID 过滤
                            .setWithPayload(Points.WithPayloadSelector.newBuilder().setEnable(true).build())
                            .setScoreThreshold(0.5f) // 降低阈值以提高召回率
                            .build();

                    List<Points.ScoredPoint> results = qdrantClient.searchAsync(searchPoints).get();
                    log.info("RAG检索结果: count={}", results.size());

                    if (!results.isEmpty()) {
                        StringBuilder contextBuilder = new StringBuilder();
                        contextBuilder.append("以下是参考资料：\n\n");

                        for (Points.ScoredPoint point : results) {
                            String content = point.getPayloadMap().get("content").getStringValue();
                            String title = point.getPayloadMap().get("title").getStringValue();
                            long sourceId = point.getPayloadMap().get("sourceId").getIntegerValue();
                            String sourceType = point.getPayloadMap().containsKey("sourceType")
                                    ? point.getPayloadMap().get("sourceType").getStringValue()
                                    : "CONTENT";

                            log.info("RAG检索命中: title={}, sourceId={}, sourceType={}, score={}",
                                    title, sourceId, sourceType, point.getScore());

                            contextBuilder.append("--- [").append(title).append("] ---\n");
                            contextBuilder.append(content).append("\n\n");

                            references.add(new NoteReferenceDto(
                                    sourceId,
                                    title,
                                    content.length() > 100 ? content.substring(0, 100) + "..." : content,
                                    (double) point.getScore()));
                        }

                        contextBuilder.append("请基于以上参考资料回答用户的问题。如果资料不足，请用你的通用知识补充。问题：\n");
                        contextBuilder.append(dto.getContent());
                        finalPrompt = contextBuilder.toString();
                    } else {
                        log.info("RAG检索无结果，使用原始 prompt");
                    }
                }
            } catch (Exception e) {
                log.warn("RAG检索失败: {}", e.getMessage(), e);
                // 降级：继续使用原始 prompt
            }
        }

        // 3. 调用 AI
        String aiResponseText;
        try {
            aiResponseText = chatModel.generate(finalPrompt);
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
    public List<String> extractTags(String content) {
        if (!StringUtils.hasText(content)) {
            return new ArrayList<>();
        }
        try {
            String prompt = "请从以下内容中提取3-5个关键标签，只返回标签，用逗号分隔，不要包含其他文字：\n\n" +
                    (content.length() > 2000 ? content.substring(0, 2000) : content);
            String response = chatModel.generate(prompt);

            // 处理结果
            if (StringUtils.hasText(response)) {
                String[] tags = response.replace("，", ",").split(",");
                List<String> result = new ArrayList<>();
                for (String tag : tags) {
                    String cleanTag = tag.trim();
                    if (StringUtils.hasText(cleanTag)) {
                        result.add(cleanTag);
                    }
                }
                return result;
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("提取标签失败", e);
            return new ArrayList<>();
        }
    }

    @Override
    public com.ray.knowledgeflow.module.ai.dto.AiAnalysisResultDto generateDeepAnalysis(String content,
            String targetTypeStr, Long targetId) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        try {
            // Check cache
            com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType targetType = null;
            try {
                if (StringUtils.hasText(targetTypeStr)) {
                    targetType = com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType
                            .valueOf(targetTypeStr.toUpperCase());
                }
            } catch (IllegalArgumentException e) {
                log.warn("Invalid target type: {}", targetTypeStr);
            }

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
    public SseEmitter streamDeepAnalysis(String content, String targetTypeStr, Long targetId) {
        SseEmitter emitter = new SseEmitter(180000L); // 3 minutes timeout

        if (!StringUtils.hasText(content)) {
            try {
                emitter.complete();
            } catch (Exception e) {
                // ignore
            }
            return emitter;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                // Check cache
                com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType targetType = null;
                try {
                    if (StringUtils.hasText(targetTypeStr)) {
                        targetType = com.ray.knowledgeflow.module.ai.enums.AiAnalysisTargetType
                                .valueOf(targetTypeStr.toUpperCase());
                    }
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid target type: {}", targetTypeStr);
                }

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

        return emitter;
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
