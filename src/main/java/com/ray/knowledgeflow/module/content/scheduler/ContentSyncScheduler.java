package com.ray.knowledgeflow.module.content.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.message.ContentFetchMessage;
import com.ray.knowledgeflow.module.content.message.ContentFetchProducer;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;

/**
 * 内容同步调度器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ContentSyncScheduler {

    private final ContentSourceMapper contentSourceMapper;
    private final ContentFetchProducer contentFetchProducer;

    @Value("${app.sync.rss.enabled:true}")
    private boolean rssEnabled;
    @Value("${app.sync.github.enabled:true}")
    private boolean githubEnabled;
    @Value("${app.sync.arxiv.enabled:true}")
    private boolean arxivEnabled;

    /**
     * 每6小时同步一次RSS源
     */
    @Scheduled(cron = "${app.sync.rss.cron:0 0 */6 * * ?}")
    public void syncRssSources() {
        if (!rssEnabled) {
            return;
        }
        log.info("开始定时同步RSS源");
        List<ContentSource> sources = contentSourceMapper.selectList(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getType, "RSS")
                .eq(ContentSource::getStatus, 1));

        for (ContentSource source : sources) {
            try {
                submit(source);
            } catch (Exception e) {
                log.error("RSS同步失败: source={}", source.getName(), e);
            }
        }
        log.info("RSS源同步任务完成，共处理{}个源", sources.size());
    }

    /**
     * 每天凌晨2点同步GitHub starred
     */
    @Scheduled(cron = "${app.sync.github.cron:0 0 2 * * ?}")
    public void syncGitHubSources() {
        if (!githubEnabled) {
            return;
        }
        log.info("开始定时同步GitHub源");
        List<ContentSource> sources = contentSourceMapper.selectList(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getType, "GITHUB")
                .eq(ContentSource::getStatus, 1));

        for (ContentSource source : sources) {
            try {
                submit(source);
            } catch (Exception e) {
                log.error("GitHub同步失败: source={}", source.getName(), e);
            }
        }
        log.info("GitHub源同步任务完成，共处理{}个源", sources.size());
    }

    /**
     * 每天凌晨2:30同步arXiv
     */
    @Scheduled(cron = "${app.sync.arxiv.cron:0 30 2 * * ?}")
    public void syncArXivSources() {
        if (!arxivEnabled) {
            return;
        }
        log.info("开始定时同步arXiv源");
        List<ContentSource> sources = contentSourceMapper.selectList(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getType, "ARXIV")
                .eq(ContentSource::getStatus, 1));

        for (ContentSource source : sources) {
            try {
                submit(source);
            } catch (Exception e) {
                log.error("arXiv同步失败: source={}", source.getName(), e);
            }
        }
        log.info("arXiv源同步任务完成，共处理{}个源", sources.size());
    }

    private void submit(ContentSource source) {
        contentFetchProducer.sendFetchTask(new ContentFetchMessage(
                source.getType(), source.getId(), source.getUserId(), null));
    }
}
