package com.ray.knowledgeflow.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 配置类
 * 定义队列、交换机、绑定关系和消息转换器
 *
 * @author Ray
 * @since 2024-01-20
 */
@Configuration
public class RabbitMQConfig {

    // ==================== 队列名称常量 ====================

    /**
     * 内容抓取队列
     */
    public static final String CONTENT_FETCH_QUEUE = "content.fetch.queue";

    /**
     * AI 处理队列
     */
    public static final String AI_PROCESS_QUEUE = "ai.process.queue";

    /**
     * 索引更新队列
     */
    public static final String INDEX_UPDATE_QUEUE = "index.update.queue";

    public static final String CONTENT_FETCH_DLQ = "content.fetch.dlq";
    public static final String AI_PROCESS_DLQ = "ai.process.dlq";
    public static final String INDEX_UPDATE_DLQ = "index.update.dlq";

    /**
     * 学习提醒队列
     */
    public static final String LEARNING_REMINDER_QUEUE = "learning.reminder.queue";

    /**
     * 互动计数同步队列（点赞数写入数据库后，异步刷新 Redis 缓存）
     */
    public static final String ENGAGEMENT_SYNC_QUEUE = "engagement.sync.queue";

    /**
     * 互动计数同步死信队列，重试耗尽的消息进入此处，便于排查与手工补偿
     */
    public static final String ENGAGEMENT_SYNC_DLQ = "engagement.sync.dlq";

    // ==================== 交换机名称常量 ====================

    public static final String CONTENT_EXCHANGE = "content.exchange";
    public static final String AI_EXCHANGE = "ai.exchange";
    public static final String INDEX_EXCHANGE = "index.exchange";
    public static final String LEARNING_EXCHANGE = "learning.exchange";
    public static final String ENGAGEMENT_EXCHANGE = "engagement.exchange";
    public static final String ENGAGEMENT_DLX = "engagement.dlx";
    public static final String FAILED_TASK_DLX = "task.failed.dlx";

    // ==================== 路由键常量 ====================

    public static final String CONTENT_FETCH_ROUTING_KEY = "content.fetch";
    public static final String AI_PROCESS_ROUTING_KEY = "ai.process";
    public static final String INDEX_UPDATE_ROUTING_KEY = "index.update";
    public static final String LEARNING_REMINDER_ROUTING_KEY = "learning.reminder";
    public static final String ENGAGEMENT_SYNC_ROUTING_KEY = "engagement.sync";
    public static final String ENGAGEMENT_DLQ_ROUTING_KEY = "engagement.dlq";
    public static final String CONTENT_FETCH_DLQ_ROUTING_KEY = "content.fetch.failed";
    public static final String AI_PROCESS_DLQ_ROUTING_KEY = "ai.process.failed";
    public static final String INDEX_UPDATE_DLQ_ROUTING_KEY = "index.update.failed";

    // ==================== 消息转换器 ====================

    /**
     * JSON 消息转换器
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 配置 RabbitTemplate
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    // ==================== 队列定义 ====================

    @Bean
    public Queue contentFetchQueue() {
        return QueueBuilder.durable(CONTENT_FETCH_QUEUE)
                .deadLetterExchange(FAILED_TASK_DLX)
                .deadLetterRoutingKey(CONTENT_FETCH_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue aiProcessQueue() {
        return QueueBuilder.durable(AI_PROCESS_QUEUE)
                .deadLetterExchange(FAILED_TASK_DLX)
                .deadLetterRoutingKey(AI_PROCESS_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue indexUpdateQueue() {
        return QueueBuilder.durable(INDEX_UPDATE_QUEUE)
                .deadLetterExchange(FAILED_TASK_DLX)
                .deadLetterRoutingKey(INDEX_UPDATE_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue contentFetchDlq() {
        return QueueBuilder.durable(CONTENT_FETCH_DLQ).build();
    }

    @Bean
    public Queue aiProcessDlq() {
        return QueueBuilder.durable(AI_PROCESS_DLQ).build();
    }

    @Bean
    public Queue indexUpdateDlq() {
        return QueueBuilder.durable(INDEX_UPDATE_DLQ).build();
    }

    @Bean
    public Queue learningReminderQueue() {
        return QueueBuilder.durable(LEARNING_REMINDER_QUEUE).build();
    }

    /**
     * 互动计数同步队列，重试耗尽后转入死信交换机而不是被丢弃
     */
    @Bean
    public Queue engagementSyncQueue() {
        return QueueBuilder.durable(ENGAGEMENT_SYNC_QUEUE)
                .deadLetterExchange(ENGAGEMENT_DLX)
                .deadLetterRoutingKey(ENGAGEMENT_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue engagementSyncDlq() {
        return QueueBuilder.durable(ENGAGEMENT_SYNC_DLQ).build();
    }

    // ==================== 交换机定义 ====================

    @Bean
    public DirectExchange contentExchange() {
        return new DirectExchange(CONTENT_EXCHANGE);
    }

    @Bean
    public DirectExchange aiExchange() {
        return new DirectExchange(AI_EXCHANGE);
    }

    @Bean
    public DirectExchange indexExchange() {
        return new DirectExchange(INDEX_EXCHANGE);
    }

    @Bean
    public DirectExchange learningExchange() {
        return new DirectExchange(LEARNING_EXCHANGE);
    }

    @Bean
    public DirectExchange engagementExchange() {
        return new DirectExchange(ENGAGEMENT_EXCHANGE);
    }

    @Bean
    public DirectExchange engagementDlx() {
        return new DirectExchange(ENGAGEMENT_DLX);
    }

    @Bean
    public DirectExchange failedTaskDlx() {
        return new DirectExchange(FAILED_TASK_DLX);
    }

    // ==================== 绑定关系 ====================

    @Bean
    public Binding contentFetchBinding(Queue contentFetchQueue, DirectExchange contentExchange) {
        return BindingBuilder.bind(contentFetchQueue)
                .to(contentExchange)
                .with(CONTENT_FETCH_ROUTING_KEY);
    }

    @Bean
    public Binding aiProcessBinding(Queue aiProcessQueue, DirectExchange aiExchange) {
        return BindingBuilder.bind(aiProcessQueue)
                .to(aiExchange)
                .with(AI_PROCESS_ROUTING_KEY);
    }

    @Bean
    public Binding indexUpdateBinding(Queue indexUpdateQueue, DirectExchange indexExchange) {
        return BindingBuilder.bind(indexUpdateQueue)
                .to(indexExchange)
                .with(INDEX_UPDATE_ROUTING_KEY);
    }

    @Bean
    public Binding learningReminderBinding(Queue learningReminderQueue, DirectExchange learningExchange) {
        return BindingBuilder.bind(learningReminderQueue)
                .to(learningExchange)
                .with(LEARNING_REMINDER_ROUTING_KEY);
    }

    @Bean
    public Binding engagementSyncBinding(Queue engagementSyncQueue, DirectExchange engagementExchange) {
        return BindingBuilder.bind(engagementSyncQueue)
                .to(engagementExchange)
                .with(ENGAGEMENT_SYNC_ROUTING_KEY);
    }

    @Bean
    public Binding engagementDlqBinding(Queue engagementSyncDlq, DirectExchange engagementDlx) {
        return BindingBuilder.bind(engagementSyncDlq)
                .to(engagementDlx)
                .with(ENGAGEMENT_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding contentFetchDlqBinding(Queue contentFetchDlq, DirectExchange failedTaskDlx) {
        return BindingBuilder.bind(contentFetchDlq)
                .to(failedTaskDlx)
                .with(CONTENT_FETCH_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding aiProcessDlqBinding(Queue aiProcessDlq, DirectExchange failedTaskDlx) {
        return BindingBuilder.bind(aiProcessDlq)
                .to(failedTaskDlx)
                .with(AI_PROCESS_DLQ_ROUTING_KEY);
    }

    @Bean
    public Binding indexUpdateDlqBinding(Queue indexUpdateDlq, DirectExchange failedTaskDlx) {
        return BindingBuilder.bind(indexUpdateDlq)
                .to(failedTaskDlx)
                .with(INDEX_UPDATE_DLQ_ROUTING_KEY);
    }
}
