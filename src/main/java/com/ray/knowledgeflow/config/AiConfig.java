package com.ray.knowledgeflow.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * AI 模型配置
 * - 嵌入模型：智谱 AI (embedding-3)
 * - 聊天模型：DeepSeek (deepseek-chat) 通过 OpenAI 兼容 API
 *
 * @author Ray
 * @since 2024-01-21
 */
@Configuration
public class AiConfig {

    @Value("${ZHIPUAI_API_KEY:}")
    private String zhipuApiKey;

    @Value("${langchain4j.open-ai.base-url:https://api.deepseek.com}")
    private String deepseekBaseUrl;

    @Value("${langchain4j.open-ai.api-key:}")
    private String deepseekApiKey;

    @Value("${langchain4j.open-ai.model-name:deepseek-chat}")
    private String deepseekModelName;

    @Value("${langchain4j.open-ai.temperature:0.7}")
    private Double temperature;

    /**
     * 配置 Zhipu AI 嵌入模型 (embedding-3) - 用于 RAG 向量化
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel.builder()
                .apiKey(zhipuApiKey)
                .model("embedding-3")
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
