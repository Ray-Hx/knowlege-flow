package com.ray.knowledgeflow.module.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.ai.entity.AiMessage;
import com.ray.knowledgeflow.module.ai.mapper.AiMessageMapper;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageType;
import dev.langchain4j.data.message.UserMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 对话记忆测试：覆盖时间序还原、长消息截断，以及历史序列的规整。
 *
 * <p>规整是重点：流式生成失败不落库回复，会留下孤儿提问；窗口按条数截断，
 * 首条可能落在 assistant 上。两者都会破坏 user / assistant 交替。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ConversationMemoryTest {

    @Mock
    private AiMessageMapper messageMapper;

    @InjectMocks
    private ConversationMemory memory;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(memory, "windowSize", 10);
        ReflectionTestUtils.setField(memory, "maxMessageChars", 1000);
    }

    private static AiMessage msg(long id, String role, String content) {
        AiMessage m = new AiMessage();
        m.setId(id);
        m.setRole(role);
        m.setContent(content);
        return m;
    }

    /** mapper 按 id 倒序返回，模拟 SQL 的 ORDER BY id DESC */
    @SuppressWarnings("unchecked")
    private void givenStored(AiMessage... ascending) {
        List<AiMessage> descending = new ArrayList<>(List.of(ascending));
        java.util.Collections.reverse(descending);
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(descending);
    }

    @Test
    void load_ShouldReturnChronologicalOrderWithMappedRoles() {
        givenStored(
                msg(1, "user", "什么是 RRF 融合？"),
                msg(2, "assistant", "RRF 是倒数排名融合……"));

        List<ChatMessage> history = memory.load(1L);

        assertEquals(2, history.size());
        assertEquals(ChatMessageType.USER, history.get(0).type());
        assertEquals("什么是 RRF 融合？", history.get(0).text());
        assertEquals(ChatMessageType.AI, history.get(1).type());
    }

    @Test
    void load_ShouldReturnEmptyForNewConversation() {
        when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
        assertTrue(memory.load(1L).isEmpty());
    }

    @Test
    void load_ShouldTruncateOverlongMessage() {
        ReflectionTestUtils.setField(memory, "maxMessageChars", 10);
        givenStored(
                msg(1, "user", "问题"),
                msg(2, "assistant", "0123456789ABCDEF"));

        List<ChatMessage> history = memory.load(1L);

        assertEquals("0123456789……（历史消息过长，已截断）", history.get(1).text());
    }

    @Test
    void load_WhenWindowDisabled_ShouldSkipQueryEntirely() {
        ReflectionTestUtils.setField(memory, "windowSize", 0);
        assertTrue(memory.load(1L).isEmpty());
        org.mockito.Mockito.verify(messageMapper, org.mockito.Mockito.never())
                .selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void normalize_ShouldDropLeadingAssistantFromTruncatedWindow() {
        // 窗口边界切在半轮上：首条是上一轮的回答，没有配对的提问
        List<ChatMessage> normalized = ConversationMemory.normalize(new ArrayList<>(List.of(
                dev.langchain4j.data.message.AiMessage.from("上一轮的回答"),
                UserMessage.from("问题"),
                dev.langchain4j.data.message.AiMessage.from("回答"))));

        assertEquals(2, normalized.size());
        assertEquals(ChatMessageType.USER, normalized.get(0).type());
        assertEquals("问题", normalized.get(0).text());
    }

    @Test
    void normalize_ShouldDropTrailingUnansweredQuestion() {
        // 流式生成失败：提问已落库，回复没有。末条留成 user 会和随后追加的当前提问撞角色
        List<ChatMessage> normalized = ConversationMemory.normalize(new ArrayList<>(List.of(
                UserMessage.from("问题"),
                dev.langchain4j.data.message.AiMessage.from("回答"),
                UserMessage.from("生成失败的提问"))));

        assertEquals(2, normalized.size());
        assertEquals(ChatMessageType.AI, normalized.get(1).type());
    }

    @Test
    void normalize_ShouldKeepLatestOfConsecutiveSameRole() {
        // 连续两次提问都没拿到回答，只有最后一次值得保留
        List<ChatMessage> normalized = ConversationMemory.normalize(new ArrayList<>(List.of(
                UserMessage.from("失败的提问"),
                UserMessage.from("重试的提问"),
                dev.langchain4j.data.message.AiMessage.from("回答"))));

        assertEquals(2, normalized.size());
        assertEquals("重试的提问", normalized.get(0).text());
    }

    @Test
    void normalize_ShouldProduceStrictlyAlternatingSequence() {
        List<ChatMessage> normalized = ConversationMemory.normalize(new ArrayList<>(List.of(
                dev.langchain4j.data.message.AiMessage.from("孤儿回答"),
                UserMessage.from("问 1"),
                UserMessage.from("问 1 重试"),
                dev.langchain4j.data.message.AiMessage.from("答 1"),
                UserMessage.from("问 2"),
                dev.langchain4j.data.message.AiMessage.from("答 2"),
                UserMessage.from("未答的问 3"))));

        assertEquals(ChatMessageType.USER, normalized.get(0).type());
        assertEquals(ChatMessageType.AI, normalized.get(normalized.size() - 1).type());
        for (int i = 1; i < normalized.size(); i++) {
            org.junit.jupiter.api.Assertions.assertNotEquals(
                    normalized.get(i - 1).type(), normalized.get(i).type(),
                    "相邻消息角色必须交替");
        }
    }

    @Test
    void normalize_ShouldTolerateSingleOrphanMessage() {
        assertTrue(ConversationMemory.normalize(new ArrayList<>(List.of(
                UserMessage.from("只有提问，没有回答")))).isEmpty());
        assertTrue(ConversationMemory.normalize(new ArrayList<>()).isEmpty());
    }
}
