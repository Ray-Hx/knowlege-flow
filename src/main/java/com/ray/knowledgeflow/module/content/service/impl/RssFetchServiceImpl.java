package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.content.service.IRssFetchService;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * RSS 抓取服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RssFetchServiceImpl implements IRssFetchService {

    private final ContentItemMapper contentItemMapper;
    private final ContentSourceMapper contentSourceMapper; // Added
    private final ApplicationEventPublisher eventPublisher; // Changed type to non-qualified

    @Override
    @Async
    public void fetchAsync(ContentSource source) {
        log.info("开始异步抓取RSS源: {}", source.getName());
        try {
            int count = fetch(source);
            log.info("RSS 抓取完成: source={}, newItems={}", source.getName(), count);
        } catch (Exception e) {
            log.error("RSS 抓取失败: source={}", source.getName(), e);
        }
    }

    @Override
    public void fetchFromSource(Long sourceId, Long userId) {
        // 查询内容源并调用 fetchAsync
        ContentSource source = contentSourceMapper.selectById(sourceId);
        if (source != null && source.getUserId().equals(userId)) {
            fetchAsync(source);
        } else {
            log.warn("RSS内容源不存在或无权访问: sourceId={}, userId={}", sourceId, userId);
        }
    }

    @Override
    public int fetch(ContentSource source) {
        int newCount = 0;

        log.info("开始抓取RSS源: name={}, url={}", source.getName(), source.getUrl());

        try {
            // 手动处理重定向 (包括 HTTP→HTTPS)
            java.io.InputStream stream = openConnectionWithRedirect(source.getUrl());

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed;
            try {
                feed = input.build(new XmlReader(stream));
            } finally {
                stream.close();
            }

            List<SyndEntry> entries = feed.getEntries();
            log.info("RSS源解析成功: name={}, 共{}条目", source.getName(), entries.size());

            for (SyndEntry entry : entries) {
                // 检查是否已存在
                String link = entry.getLink();
                if (link == null || link.isBlank()) {
                    log.warn("RSS条目没有链接，跳过: title={}", entry.getTitle());
                    continue;
                }

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
                item.setTitle(entry.getTitle() != null ? entry.getTitle() : "无标题");
                item.setUrl(link);
                item.setAuthor(entry.getAuthor());
                item.setPublishTime(toLocalDateTime(entry.getPublishedDate()));
                item.setStatus(0); // 未读

                // 提取摘要 (去除 HTML 标签)
                if (entry.getDescription() != null) {
                    String desc = entry.getDescription().getValue();
                    String plainText = Jsoup.parse(desc).text();
                    item.setSummary(plainText.length() > 500 ? plainText.substring(0, 500) + "..." : plainText);
                }

                // 提取全文内容
                if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                    String content = entry.getContents().get(0).getValue();
                    item.setContentText(Jsoup.parse(content).text());
                }

                contentItemMapper.insert(item);

                // 发布事件
                eventPublisher
                        .publishEvent(new com.ray.knowledgeflow.module.search.event.ContentSavedEvent(this, item));
                newCount++;
            }

            log.info("RSS抓取完成: name={}, 新增{}条", source.getName(), newCount);

        } catch (java.net.SocketTimeoutException e) {
            log.error("RSS 抓取超时: url={}", source.getUrl(), e);
            throw new RuntimeException("RSS 抓取超时: " + source.getUrl());
        } catch (java.io.IOException e) {
            log.error("RSS 网络错误: url={}, error={}", source.getUrl(), e.getMessage());
            throw new RuntimeException("RSS 网络错误: " + e.getMessage());
        } catch (Exception e) {
            log.error("解析 RSS 失败: url={}", source.getUrl(), e);
            throw new RuntimeException("RSS 解析失败: " + e.getMessage());
        }

        return newCount;
    }

    /**
     * 打开连接并手动处理重定向（包括 HTTP→HTTPS）
     * Java的setInstanceFollowRedirects不支持跨协议重定向
     */
    private java.io.InputStream openConnectionWithRedirect(String urlString) throws java.io.IOException {
        int maxRedirects = 5;
        String currentUrl = urlString;

        for (int i = 0; i < maxRedirects; i++) {
            URL url = new URL(currentUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);

            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false); // 手动处理重定向

            int responseCode = conn.getResponseCode();
            log.info("RSS连接响应: url={}, responseCode={}", currentUrl, responseCode);

            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                return conn.getInputStream();
            }

            if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == 307 || responseCode == 308) {
                String location = conn.getHeaderField("Location");
                if (location == null || location.isEmpty()) {
                    throw new java.io.IOException("重定向但没有Location头: " + responseCode);
                }
                log.info("RSS重定向: {} -> {}", currentUrl, location);
                currentUrl = location;
                conn.disconnect();
                continue;
            }

            throw new java.io.IOException("RSS服务器返回错误: HTTP " + responseCode);
        }

        throw new java.io.IOException("重定向次数超过限制: " + maxRedirects);
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
