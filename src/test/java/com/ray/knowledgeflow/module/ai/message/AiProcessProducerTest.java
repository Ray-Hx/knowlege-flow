package com.ray.knowledgeflow.module.ai.message;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * AI 处理消息生产者的容错测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiProcessProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;
    @Mock
    private SetOperations<String, String> setOps;

    @InjectMocks
    private AiProcessProducer producer;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        // 去重键未占用，让消息真正走到发送逻辑
        when(valueOps.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
    }

    /**
     * MQ 不可用时不得向上抛出。
     *
     * <p>摘要、标签、图谱都是对笔记的异步增强，而调用方 createNote 带 @Transactional，
     * 抛出会导致整个事务回滚，用户刚写完的笔记会因为 MQ 抖动而丢失。
     */
    @Test
    void sendSummaryTask_WhenBrokerDown_ShouldNotPropagate() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> producer.sendSummaryTask(1L, 2L, "标题", "正文"),
                "MQ 故障不能让笔记创建事务回滚");
    }

    @Test
    void sendTagExtractionTask_WhenBrokerDown_ShouldNotPropagate() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> producer.sendTagExtractionTask(1L, 2L, "标题", "正文"));
    }

    /**
     * 图谱任务走 Redis 集合标记（由定时任务批量消费），不经过 MQ。
     * Redis 故障同样不能让笔记创建回滚。
     */
    @Test
    void sendGraphBuildTask_WhenRedisDown_ShouldNotPropagate() {
        when(redisTemplate.opsForSet()).thenReturn(setOps);
        doThrow(new RedisConnectionFailureException("Redis 不可用"))
                .when(setOps).add(anyString(), any(String[].class));

        assertDoesNotThrow(() -> producer.sendGraphBuildTask(1L, 2L, "标题", "正文"),
                "Redis 故障不能让笔记创建事务回滚");
    }
}
