package com.ray.knowledgeflow.module.search.message;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

/**
 * 索引消息生产者的容错测试。
 */
@ExtendWith(MockitoExtension.class)
class IndexUpdateProducerTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private IndexUpdateProducer producer;

    /**
     * MQ 不可用时不得向上抛出。
     *
     * <p>调用方 NoteServiceImpl.createNote 带 @Transactional，
     * 一旦抛出会连带回滚，用户刚写的笔记就没了——而建索引只是异步增强，
     * 迟一点甚至漏掉都可以靠 /note/rebuild-index 补回来。
     */
    @Test
    void sendIndexUpdateTask_WhenBrokerDown_ShouldNotPropagate() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> producer.sendCreateIndex(
                "NOTE", 1L, 2L, "标题", "正文", "摘要", "标签"),
                "MQ 故障不能让笔记创建事务回滚");
    }

    @Test
    void sendDeleteIndex_WhenBrokerDown_ShouldNotPropagate() {
        doThrow(new AmqpConnectException(new RuntimeException("Connection refused")))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        assertDoesNotThrow(() -> producer.sendDeleteIndex("NOTE", 1L, 2L));
    }
}
