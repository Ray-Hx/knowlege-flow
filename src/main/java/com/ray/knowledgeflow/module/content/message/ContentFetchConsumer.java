package com.ray.knowledgeflow.module.content.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import com.ray.knowledgeflow.module.content.service.IArXivFetchService;
import com.ray.knowledgeflow.module.content.service.IGitHubFetchService;
import com.ray.knowledgeflow.module.content.service.IRssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;

import java.time.LocalDateTime;

/**
 * 内容抓取消息消费者
 *
 * @author Ray
 * @since 2024-01-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentFetchConsumer {

    private final IRssFetchService rssFetchService;
    private final IGitHubFetchService gitHubFetchService;
    private final IArXivFetchService arXivFetchService;
    private final ContentSourceMapper contentSourceMapper;

    /**
     * 监听内容抓取队列
     *
     * @param message 抓取任务消息
     */
    @RabbitListener(queues = RabbitMQConfig.CONTENT_FETCH_QUEUE)
    public void handleFetchTask(ContentFetchMessage message) {
        if (message == null || message.getType() == null || message.getType().isBlank()
                || message.getSourceId() == null || message.getUserId() == null) {
            throw new IllegalArgumentException("内容抓取消息缺少必要字段");
        }

        log.info("收到内容抓取任务: type={}, sourceId={}, userId={}",
                message.getType(), message.getSourceId(), message.getUserId());

        try {
            switch (message.getType().toUpperCase()) {
                case "RSS":
                    rssFetchService.fetchFromSource(message.getSourceId(), message.getUserId());
                    log.info("RSS 抓取完成: sourceId={}", message.getSourceId());
                    break;

                case "GITHUB":
                    gitHubFetchService.fetchFromSource(message.getSourceId(), message.getUserId());
                    log.info("GitHub 抓取完成: sourceId={}", message.getSourceId());
                    break;

                case "ARXIV":
                    arXivFetchService.fetchFromSource(message.getSourceId(), message.getUserId());
                    log.info("arXiv 抓取完成: sourceId={}", message.getSourceId());
                    break;

                default:
                    throw new IllegalArgumentException("未知的内容源类型: " + message.getType());
            }
            ContentSource update = new ContentSource();
            update.setLastSyncTime(LocalDateTime.now());
            contentSourceMapper.update(update, new LambdaUpdateWrapper<ContentSource>()
                    .eq(ContentSource::getId, message.getSourceId())
                    .eq(ContentSource::getUserId, message.getUserId()));
        } catch (Exception e) {
            log.error("内容抓取失败: type={}, sourceId={}", message.getType(), message.getSourceId(), e);
            // 将失败传给 Spring AMQP，使配置的消费重试生效，避免静默丢任务。
            if (e instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("内容抓取失败", e);
        }
    }
}
