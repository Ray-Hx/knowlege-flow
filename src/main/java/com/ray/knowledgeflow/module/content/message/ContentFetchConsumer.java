package com.ray.knowledgeflow.module.content.message;

import com.ray.knowledgeflow.config.RabbitMQConfig;
import com.ray.knowledgeflow.module.content.service.IArXivFetchService;
import com.ray.knowledgeflow.module.content.service.IGitHubFetchService;
import com.ray.knowledgeflow.module.content.service.IRssFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

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

    /**
     * 监听内容抓取队列
     *
     * @param message 抓取任务消息
     */
    @RabbitListener(queues = RabbitMQConfig.CONTENT_FETCH_QUEUE)
    public void handleFetchTask(ContentFetchMessage message) {
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
                    log.warn("未知的内容源类型: {}", message.getType());
            }
        } catch (Exception e) {
            log.error("内容抓取失败: type={}, sourceId={}", message.getType(), message.getSourceId(), e);
            // TODO: 可以考虑重试机制或发送到死信队列
        }
    }
}
