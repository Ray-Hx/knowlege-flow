package com.ray.knowledgeflow.module.content.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.service.IArXivFetchService;
import com.ray.knowledgeflow.module.content.service.IGitHubFetchService;
import com.ray.knowledgeflow.module.content.service.IRssFetchService;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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
    private final IRssFetchService rssFetchService;
    private final IGitHubFetchService gitHubFetchService;
    private final IArXivFetchService arXivFetchService;

    /**
     * 每6小时同步一次RSS源
     */
    @Scheduled(cron = "0 0 */6 * * ?")
    public void syncRssSources() {
        log.info("开始定时同步RSS源");
        List<ContentSource> sources = contentSourceMapper.selectList(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getType, "RSS")
                .eq(ContentSource::getStatus, 1));

        for (ContentSource source : sources) {
            try {
                rssFetchService.fetchAsync(source);
                source.setLastSyncTime(LocalDateTime.now());
                contentSourceMapper.updateById(source);
            } catch (Exception e) {
                log.error("RSS同步失败: source={}", source.getName(), e);
            }
        }
        log.info("RSS源同步任务完成，共处理{}个源", sources.size());
    }

    /**
     * 每天凌晨2点同步GitHub starred
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void syncGitHubSources() {
        log.info("开始定时同步GitHub源");
        List<ContentSource> sources = contentSourceMapper.selectList(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getType, "GITHUB")
                .eq(ContentSource::getStatus, 1));

        for (ContentSource source : sources) {
            try {
                gitHubFetchService.fetchAsync(source);
                source.setLastSyncTime(LocalDateTime.now());
                contentSourceMapper.updateById(source);
            } catch (Exception e) {
                log.error("GitHub同步失败: source={}", source.getName(), e);
            }
        }
        log.info("GitHub源同步任务完成，共处理{}个源", sources.size());
    }

    /**
     * 每天凌晨2:30同步arXiv
     */
    @Scheduled(cron = "0 30 2 * * ?")
    public void syncArXivSources() {
        log.info("开始定时同步arXiv源");
        List<ContentSource> sources = contentSourceMapper.selectList(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getType, "ARXIV")
                .eq(ContentSource::getStatus, 1));

        for (ContentSource source : sources) {
            try {
                arXivFetchService.fetchAsync(source);
                source.setLastSyncTime(LocalDateTime.now());
                contentSourceMapper.updateById(source);
            } catch (Exception e) {
                log.error("arXiv同步失败: source={}", source.getName(), e);
            }
        }
        log.info("arXiv源同步任务完成，共处理{}个源", sources.size());
    }
}
