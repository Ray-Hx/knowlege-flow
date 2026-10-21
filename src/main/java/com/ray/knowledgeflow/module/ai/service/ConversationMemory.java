package com.ray.knowledgeflow.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.ai.entity.AiMessage;
import com.ray.knowledgeflow.module.ai.mapper.AiMessageMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 对话记忆。
 *
 * <p>此前问答只把当前这一轮的 prompt 交给模型：历史消息虽然落了库，但仅用于前端渲染，
 * 从未回灌给大模型。表现为多轮对话的界面下面跑的是一次次互不相干的单轮调用，
 * 追问里的指代（"它""这个方案"）模型无从解析。本组件负责把落库的历史还原成模型可消费的消息列表。
 *
 * <p>同步问答与流式问答都要用，故与 {@link RagRetriever} 一样抽成独立组件。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMemory {

    private static final String ROLE_ASSISTANT = "assistant";

    private final AiMessageMapper messageMapper;

    /** 回灌的历史消息条数上限。窗口过大既推高 token 成本，也会稀释当前问题的权重 */
    @Value("${app.ai.memory.window-size:10}")
    private int windowSize;

    /**
     * 单条历史消息保留的字符数上限。
     *
     * <p>AI 回答动辄上千字，整窗原样回灌会挤占上下文。截断的代价是
     * "把上面第三点展开讲讲"这类追问可能丢失细节，权衡后优先保证上下文长度可控。
     */
    @Value("${app.ai.memory.max-message-chars:1000}")
    private int maxMessageChars;

    /**
     * 载入历史窗口，按时间正序（旧 → 新）返回。
     *
     * <p><b>必须在保存当前轮用户消息之前调用</b>，否则这一轮的提问会混进历史，
     * 模型会看到同一个问题出现两次。
     *
     * @param conversationId 对话 ID，调用方需已校验归属
     */
    public List<ChatMessage> load(Long conversationId) {
        if (windowSize <= 0) {
            return new ArrayList<>();
        }

        // 按 id 倒序取最近 N 条：createdTime 精度有限，同一秒内落库的消息排序不稳定
        List<AiMessage> recent = messageMapper.selectList(
                new LambdaQueryWrapper<AiMessage>()
                        .eq(AiMessage::getConversationId, conversationId)
                        .orderByDesc(AiMessage::getId)
                        .last("LIMIT " + windowSize));
        if (recent.isEmpty()) {
            return new ArrayList<>();
        }
        Collections.reverse(recent);

        List<ChatMessage> history = new ArrayList<>(recent.size());
        for (AiMessage msg : recent) {
            String text = truncate(msg.getContent());
            if (text.isEmpty()) {
                continue;
            }
            history.add(ROLE_ASSISTANT.equals(msg.getRole())
                    ? dev.langchain4j.data.message.AiMessage.from(text)
                    : UserMessage.from(text));
        }
        return normalize(history);
    }

    /**
     * 规整成严格交替、且首尾完整的 user / assistant 序列。
     *
     * <p>两种情况会破坏交替：流式生成失败时 onError 不落库回复，留下没有配对回答的孤儿提问；
     * 窗口按条数截断，首条可能正好落在 assistant 上。部分厂商对消息角色交替有强校验，
     * 这里统一规整后再送出。
     */
    static List<ChatMessage> normalize(List<ChatMessage> history) {
        // 从后往前只保留角色发生切换的消息：同角色连续出现时（失败重试的提问），保留最近的那条
        List<ChatMessage> kept = new ArrayList<>(history.size());
        ChatMessageType lastKept = null;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage msg = history.get(i);
            if (msg.type() != lastKept) {
                kept.add(msg);
                lastKept = msg.type();
            }
        }
        Collections.reverse(kept);

        // 首条必须是 user；末条必须是 assistant，否则会和随后追加的当前提问撞成两条连续 user
        if (!kept.isEmpty() && kept.get(0).type() != ChatMessageType.USER) {
            kept.remove(0);
        }
        if (!kept.isEmpty() && kept.get(kept.size() - 1).type() != ChatMessageType.AI) {
            kept.remove(kept.size() - 1);
        }
        return kept;
    }

    private String truncate(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.length() <= maxMessageChars) {
            return trimmed;
        }
        return trimmed.substring(0, maxMessageChars) + "……（历史消息过长，已截断）";
    }
}
