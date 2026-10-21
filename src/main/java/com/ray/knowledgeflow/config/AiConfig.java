package com.ray.knowledgeflow.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 模型配置
 * - 嵌入模型：通义千问 DashScope (text-embedding-v4，通过 OpenAI 兼容 API)
 * - 聊天模型：DeepSeek (deepseek-chat) 通过 OpenAI 兼容 API
 *
 * @author Ray
 * @since 2024-01-21
 */
@Configuration
public class AiConfig {

    @Value("${knowledge-flow.ai.qwen.api-key:${QWEN_API_KEY:}}")
    private String qwenApiKey;

    @Value("${knowledge-flow.ai.qwen.base-url:${QWEN_BASE_URL:https://dashscope.aliyuncs.com/compatible-mode/v1}}")
    private String qwenBaseUrl;

    @Value("${knowledge-flow.ai.qwen.model:${QWEN_EMBEDDING_MODEL:text-embedding-v4}}")
    private String qwenEmbeddingModel;

    @Value("${knowledge-flow.ai.qwen.dimensions:${QWEN_EMBEDDING_DIMENSIONS:2048}}")
    private Integer qwenEmbeddingDimensions;

    @Value("${langchain4j.open-ai.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${langchain4j.open-ai.api-key:}")
    private String deepseekApiKey;

    @Value("${langchain4j.open-ai.model-name:deepseek-chat}")
    private String deepseekModelName;

    @Value("${langchain4j.open-ai.temperature:0.7}")
    private Double temperature;

    /**
     * 配置通义千问文本嵌入模型 - 用于 RAG 向量化。
     * DashScope 提供 OpenAI 兼容的 /embeddings 接口，
     * 因此可复用 LangChain4j 的 OpenAiEmbeddingModel。
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(qwenBaseUrl)
                .apiKey(qwenApiKey)
                .modelName(qwenEmbeddingModel)
                .dimensions(qwenEmbeddingDimensions)
                .timeout(Duration.ofSeconds(30))
                .maxRetries(2)
                .build();
    }

    /**
     * 配置 DeepSeek 聊天模型 - 通过 OpenAI 兼容 API
     */
    @Bean
    public dev.langchain4j.model.chat.ChatLanguageModel chatLanguageModel() {
        return OpenAiChatModel.builder()
                .baseUrl(deepseekBaseUrl)
                .apiKey(deepseekApiKey)
                .modelName(deepseekModelName)
                .temperature(temperature)
                .maxTokens(4096)
                .timeout(Duration.ofSeconds(120))
                .build();
    }

    /**
     * 配置 DeepSeek 流式聊天模型 - 通过 OpenAI 兼容 API
     */
    @Bean
    public dev.langchain4j.model.chat.StreamingChatLanguageModel streamingChatLanguageModel() {
        return OpenAiStreamingChatModel.builder()
                .baseUrl(deepseekBaseUrl)
                .apiKey(deepseekApiKey)
                .modelName(deepseekModelName)
                .temperature(temperature)
                .timeout(Duration.ofSeconds(120))
                .build();
    }
}
