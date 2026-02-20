package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.common.utils.HttpClientUtil;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.service.IArXivFetchService;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.StringReader;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * arXiv抓取服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArXivFetchServiceImpl implements IArXivFetchService {

    private final ContentItemMapper contentItemMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;

    @Override
    @Async
    public void fetchAsync(ContentSource source) {
        try {
            int count = fetch(source);
            log.info("arXiv 抓取完成: source={}, newItems={}", source.getName(), count);
        } catch (Exception e) {
            log.error("arXiv 抓取失败: source={}", source.getName(), e);
        }
    }

    @Override
    public int fetch(ContentSource source) {
        int newCount = 0;

        try {
            String apiUrl = source.getUrl();
            if (apiUrl == null || apiUrl.isEmpty()) {
                throw new RuntimeException("arXiv URL不能为空");
            }

            log.info("开始抓取arXiv论文: url={}", apiUrl);

            // 发送HTTP请求获取Atom feed
            String response = HttpClientUtil.get(apiUrl);

            // 使用Rome库解析Atom格式
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new StringReader(response));

            List<SyndEntry> entries = feed.getEntries();

            for (SyndEntry entry : entries) {
                // 提取arXiv ID和URL
                String link = entry.getLink();

                // 检查是否已存在
                long exists = contentItemMapper.selectCount(new LambdaQueryWrapper<ContentItem>()
                        .eq(ContentItem::getUserId, source.getUserId())
                        .eq(ContentItem::getUrl, link));

                if (exists > 0) {
                    continue;
                }

                // 创建新内容项
                ContentItem item = new ContentItem();
                item.setSourceId(source.getId());
                item.setUserId(source.getUserId());
                item.setTitle(entry.getTitle());
                item.setUrl(link);
                item.setStatus(0); // 未读

                // 提取作者
                if (entry.getAuthors() != null && !entry.getAuthors().isEmpty()) {
                    String authors = entry.getAuthors().stream()
                            .map(author -> author.getName())
                            .limit(3)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");
                    if (entry.getAuthors().size() > 3) {
                        authors += " 等";
                    }
                    item.setAuthor(authors);
                }

                // 设置发布时间
                item.setPublishTime(toLocalDateTime(entry.getPublishedDate()));

                // 提取摘要
                if (entry.getDescription() != null) {
                    String summary = entry.getDescription().getValue();
                    String plainText = Jsoup.parse(summary).text();
                    item.setSummary(plainText.length() > 500 ? plainText.substring(0, 500) + "..." : plainText);
                }

                // 提取全文内容（包括摘要和PDF链接）
                if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                    String content = entry.getContents().get(0).getValue();
                    item.setContentText(Jsoup.parse(content).text());
                }

                // 提取分类标签
                if (entry.getCategories() != null && !entry.getCategories().isEmpty()) {
                    StringBuilder tags = new StringBuilder("[");
                    for (int i = 0; i < Math.min(5, entry.getCategories().size()); i++) {
                        if (i > 0)
                            tags.append(",");
                        tags.append("\"").append(entry.getCategories().get(i).getName()).append("\"");
                    }
                    tags.append("]");
                    item.setTags(tags.toString());
                }

                contentItemMapper.insert(item);
                eventPublisher
                        .publishEvent(new com.ray.knowledgeflow.module.search.event.ContentSavedEvent(this, item));
                newCount++;

                // 遵守arXiv API要求：请求间隔至少3秒
                Thread.sleep(3000);
            }

            log.info("arXiv抓取完成: 新增{}篇论文", newCount);

        } catch (Exception e) {
            log.error("解析arXiv数据失败", e);
            throw new RuntimeException("arXiv抓取失败: " + e.getMessage());
        }

        return newCount;
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    @Override
    public void fetchFromSource(Long sourceId, Long userId) {
        log.info("arXiv fetchFromSource 占位方法: sourceId={}, userId={}", sourceId, userId);
        // TODO: 实现根据 sourceId查询并调用 fetchAsync
    }
}
