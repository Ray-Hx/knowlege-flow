package com.ray.knowledgeflow.module.ai.service;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 检索查询改写器。
 *
 * <p>多轮追问里的问题往往不自包含——"它和 BM25 比呢？"单独拿去做 embedding，
 * 指代信息完全丢失，向量召回必然跑偏。生成端有 {@link ConversationMemory} 提供的历史，
 * 可以自行消解指代；检索端没有，所以在进 {@link RagRetriever} 之前先把问题补全。
 *
 * <p>改写是增强而非必需：任何异常都退回原始问题，与 RAG 检索失败、重排超时的降级策略保持一致。
 *
 * @author Ray
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QueryRewriter {

    private final ChatLanguageModel chatModel;

    /** 关掉后直接使用原始问题，便于对照改写带来的召回差异 */
    @Value("${app.ai.query-rewrite.enabled:true}")
    private boolean enabled;

    /** 参与改写的历史消息条数。指代通常只依赖最近一两轮，多给无益且拖慢改写 */
    @Value("${app.ai.query-rewrite.history-size:4}")
    private int historySize;

    /** 改写提示中每条历史保留的字符数。消解指代只需要话题线索，不需要完整回答 */
    @Value("${app.ai.query-rewrite.history-chars:200}")
    private int historyChars;

    /** 改写结果的字符数上限，超出说明模型没按要求只输出问题，判为不可用 */
    @Value("${app.ai.query-rewrite.max-result-chars:200}")
    private int maxResultChars;

    /**
     * 把追问改写成自包含的检索查询。
     *
     * @param rawQuery 用户原始提问
     * @param history  当前对话的历史窗口，首轮为空
     * @return 改写后的查询；未改写、被判为不可用或调用失败时返回 {@code rawQuery}
     */
    public String rewrite(String rawQuery, List<ChatMessage> history) {
        if (!enabled || history == null || history.isEmpty() || !StringUtils.hasText(rawQuery)) {
            // 首轮没有上下文可依赖，省下一次模型调用
            return rawQuery;
        }

        try {
            String rewritten = sanitize(chatModel.generate(buildPrompt(rawQuery, history)));
            if (rewritten == null) {
                log.warn("查询改写结果不可用，退回原始问题: {}", rawQuery);
                return rawQuery;
            }
            if (!rewritten.equals(rawQuery)) {
                log.info("检索查询改写: {} -> {}", rawQuery, rewritten);
            }
            return rewritten;
        } catch (Exception e) {
            log.warn("查询改写失败，退回原始问题: {}", e.getMessage());
            return rawQuery;
        }
    }

    String buildPrompt(String rawQuery, List<ChatMessage> history) {
        StringBuilder prompt = new StringBuilder()
                .append("根据下面的对话历史，把用户的最新问题改写成一个不依赖上下文、可以独立理解的检索查询。\n")
                .append("要求：\n")
                .append("1. 只输出改写后的问题本身，不要解释，不要加引号或任何前缀。\n")
                .append("2. 如果最新问题本身已经完整、不含指代，原样输出。\n")
                .append("3. 只补全指代，不要引入对话历史里没有出现过的信息。\n\n")
                .append("对话历史：\n");

        for (ChatMessage msg : tail(history, historySize)) {
            prompt.append(msg.type() == ChatMessageType.AI ? "助手：" : "用户：")
                    .append(abbreviate(msg.text(), historyChars))
                    .append('\n');
        }

        return prompt.append("\n最新问题：").append(rawQuery)
                .append("\n改写后：")
                .toString();
    }

    /**
     * 模型偶尔不守规矩，会带上解释或把问题包在引号里。取首个非空行并剥掉包裹引号，
     * 仍不合规则返回 {@code null}，由调用方降级。
     */
    String sanitize(String raw) {
        if (raw == null) {
            return null;
        }
        String result = raw.trim();
        int newline = result.indexOf('\n');
        if (newline >= 0) {
            result = result.substring(0, newline).trim();
        }
        result = result.replaceAll("^[\"'“”「『]+", "").replaceAll("[\"'“”」』]+$", "").trim();
        if (result.isEmpty() || result.length() > maxResultChars) {
            return null;
        }
        return result;
    }

    private static List<ChatMessage> tail(List<ChatMessage> list, int n) {
        if (n <= 0 || n >= list.size()) {
            return list;
        }
        return list.subList(list.size() - n, list.size());
    }

    private static String abbreviate(String text, int max) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        return trimmed.length() <= max ? trimmed : trimmed.substring(0, max) + "...";
    }
}
