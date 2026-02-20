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

    /**
     * 学习提醒队列
     */
    public static final String LEARNING_REMINDER_QUEUE = "learning.reminder.queue";

    // ==================== 交换机名称常量 ====================

    public static final String CONTENT_EXCHANGE = "content.exchange";
    public static final String AI_EXCHANGE = "ai.exchange";
    public static final String INDEX_EXCHANGE = "index.exchange";
    public static final String LEARNING_EXCHANGE = "learning.exchange";

    // ==================== 路由键常量 ====================

    public static final String CONTENT_FETCH_ROUTING_KEY = "content.fetch";
    public static final String AI_PROCESS_ROUTING_KEY = "ai.process";
    public static final String INDEX_UPDATE_ROUTING_KEY = "index.update";
    public static final String LEARNING_REMINDER_ROUTING_KEY = "learning.reminder";

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
        return QueueBuilder.durable(CONTENT_FETCH_QUEUE).build();
    }

    @Bean
    public Queue aiProcessQueue() {
        return QueueBuilder.durable(AI_PROCESS_QUEUE).build();
    }

    @Bean
    public Queue indexUpdateQueue() {
        return QueueBuilder.durable(INDEX_UPDATE_QUEUE).build();
    }

    @Bean
    public Queue learningReminderQueue() {
        return QueueBuilder.durable(LEARNING_REMINDER_QUEUE).build();
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
}
