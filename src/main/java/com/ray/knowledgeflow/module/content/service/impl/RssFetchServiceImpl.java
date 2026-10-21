package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.content.service.IRssFetchService;
import com.ray.knowledgeflow.common.utils.OutboundUrlValidator;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.net.URI;
import java.io.ByteArrayInputStream;
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

    private static final int MAX_FEED_BYTES = 5 * 1024 * 1024;
    private static final int MAX_ENTRIES_PER_FETCH = 200;

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
        ContentSource source = findOwnedSource(sourceId, userId, "RSS");
        fetch(source);
    }

    private ContentSource findOwnedSource(Long sourceId, Long userId, String expectedType) {
        ContentSource source = contentSourceMapper.selectOne(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getId, sourceId)
                .eq(ContentSource::getUserId, userId));
        if (source == null || source.getType() == null || !expectedType.equalsIgnoreCase(source.getType())) {
            throw new IllegalArgumentException("内容源不存在、无权访问或类型不匹配");
        }
        return source;
    }

    @Override
    public int fetch(ContentSource source) {
        int newCount = 0;

        log.info("开始抓取RSS源: name={}, url={}", source.getName(), source.getUrl());

        try {
            // 手动处理重定向 (包括 HTTP→HTTPS)
            byte[] feedBytes;
            try (java.io.InputStream stream = openConnectionWithRedirect(source.getUrl())) {
                feedBytes = stream.readNBytes(MAX_FEED_BYTES + 1);
            }
            if (feedBytes.length > MAX_FEED_BYTES) {
                throw new java.io.IOException("RSS 响应超过 5MB 限制");
            }

            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(new ByteArrayInputStream(feedBytes)));

            List<SyndEntry> entries = feed.getEntries().stream().limit(MAX_ENTRIES_PER_FETCH).toList();
            log.info("RSS源解析成功: name={}, 本次处理{}条", source.getName(), entries.size());

            for (SyndEntry entry : entries) {
                // 检查是否已存在
                String link = entry.getLink();
                if (link == null || link.isBlank()) {
                    log.warn("RSS条目没有链接，跳过: title={}", entry.getTitle());
                    continue;
                }
                try {
                    link = OutboundUrlValidator.requirePublicHttpUrl(link).toString();
                } catch (IllegalArgumentException e) {
                    log.warn("RSS条目 URL 不安全，跳过: title={}, reason={}", entry.getTitle(), e.getMessage());
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
                    String plainText = sanitizePlainText(Jsoup.parse(desc).text());
                    item.setSummary(plainText.length() > 500 ? plainText.substring(0, 500) + "..." : plainText);
                }

                // 提取全文内容
                boolean hasFullText = false;
                if (entry.getContents() != null && !entry.getContents().isEmpty()) {
                    String content = entry.getContents().get(0).getValue();
                    String cleanContent = sanitizePlainText(Jsoup.parse(content).text());
                    // 如果内容足够长，认为是有效正文
                    if (cleanContent.length() > 200) {
                        item.setContentText(cleanContent);
                        hasFullText = true;
                    } else if (item.getSummary() == null && cleanContent.length() > 0) {
                        item.setSummary(cleanContent);
                    }
                }

                // 如果 RSS 本身没给全文，或者全文太短了，我们主动去网页抓
                if (!hasFullText && link.startsWith("http")) {
                    try {
                        log.info("RSS条目缺正文，尝试智能抓取原网页: {}", link);
                        Document doc = fetchPublicDocument(link);

                        // 常见正文容器选择器
                        String[] selectors = {
                                "article", ".post-content", ".entry-content", ".article-content",
                                "#content", ".markdown-body", ".rich_media_content"
                        };

                        Element mainContent = null;
                        for (String selector : selectors) {
                            mainContent = doc.selectFirst(selector);
                            if (mainContent != null && mainContent.text().length() > 200) {
                                break;
                            }
                        }

                        if (mainContent != null) {
                            // 保留 HTML 格式的内部，但如果是专门存 text 可以直接用 html()
                            // 为了配合阅读器更好的沉浸格式，建议存 html
                            item.setContentText(sanitizeArticleHtml(mainContent.html(), link));
                            log.info("原网页正文抓取成功, 长度: {}", item.getContentText().length());
                        } else {
                            // 退化方案：直接取 body 文本，或者如果没有就置空
                            String bodyText = doc.body().text();
                            if (bodyText != null && bodyText.length() > 200) {
                                item.setContentText(sanitizePlainText(bodyText));
                                log.info("退化抓取 body text 成功");
                            } else {
                                // 连 body 都没有有效的正文，就用 summary 顶替吧
                                item.setContentText(item.getSummary() != null ? item.getSummary() : "该文章无法提取正文内容。");
                            }
                        }

                    } catch (Exception e) {
                        log.warn("智能抓取原网页正文失败: url={}, error={}", link, e.getMessage());
                        item.setContentText(item.getSummary() != null ? item.getSummary() : "由于原网页限制，无法提取正文内容。");
                    }
                } else if (hasFullText && item.getContentText() != null) {
                    // 如果 RSS 自带正文，为兼容内置阅读器，最好保持 HTML
                    // 原先是 Jsoup.parse(content).text()，我们改为获取原始 HTML片段
                    String content = entry.getContents().get(0).getValue();
                    item.setContentText(sanitizeArticleHtml(content, link));
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
        URI currentUri;
        try {
            currentUri = OutboundUrlValidator.requirePublicHttpUrl(urlString);
        } catch (IllegalArgumentException e) {
            throw new java.io.IOException(e.getMessage(), e);
        }

        for (int i = 0; i < maxRedirects; i++) {
            try {
                currentUri = OutboundUrlValidator.requirePublicHttpUrl(currentUri.toString());
            } catch (IllegalArgumentException e) {
                throw new java.io.IOException(e.getMessage(), e);
            }
            URL url = currentUri.toURL();
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection(java.net.Proxy.NO_PROXY);

            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(30000);
            conn.setInstanceFollowRedirects(false); // 手动处理重定向

            int responseCode = conn.getResponseCode();
            log.info("RSS连接响应: host={}, responseCode={}", currentUri.getHost(), responseCode);

            if (responseCode == java.net.HttpURLConnection.HTTP_OK) {
                return conn.getInputStream();
            }

            if (responseCode == java.net.HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == java.net.HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == java.net.HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) {
                String location = conn.getHeaderField("Location");
                if (location == null || location.isEmpty()) {
                    throw new java.io.IOException("重定向但没有Location头: " + responseCode);
                }
                currentUri = currentUri.resolve(location);
                conn.disconnect();
                continue;
            }

            throw new java.io.IOException("RSS服务器返回错误: HTTP " + responseCode);
        }

        throw new java.io.IOException("重定向次数超过限制: " + maxRedirects);
    }

    /** 抓取 RSS 条目正文，并对每一次重定向重新执行出站地址校验。 */
    private Document fetchPublicDocument(String urlString) throws java.io.IOException {
        URI currentUri;
        try {
            currentUri = OutboundUrlValidator.requirePublicHttpUrl(urlString);
        } catch (IllegalArgumentException e) {
            throw new java.io.IOException(e.getMessage(), e);
        }

        for (int i = 0; i < 5; i++) {
            try {
                currentUri = OutboundUrlValidator.requirePublicHttpUrl(currentUri.toString());
            } catch (IllegalArgumentException e) {
                throw new java.io.IOException(e.getMessage(), e);
            }

            org.jsoup.Connection.Response response = Jsoup.connect(currentUri.toString())
                    .userAgent("KnowledgeFlow/1.0")
                    .timeout(5000)
                    .maxBodySize(2 * 1024 * 1024)
                    .followRedirects(false)
                    .ignoreHttpErrors(true)
                    .execute();

            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return response.parse();
            }
            if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                String location = response.header("Location");
                if (location == null || location.isBlank()) {
                    throw new java.io.IOException("网页重定向缺少 Location");
                }
                currentUri = currentUri.resolve(location);
                continue;
            }
            throw new java.io.IOException("网页服务器返回错误: HTTP " + status);
        }
        throw new java.io.IOException("网页重定向次数超过限制");
    }

    private String sanitizeArticleHtml(String html, String baseUri) {
        Safelist safelist = Safelist.relaxed()
                .addTags("article", "section", "figure", "figcaption")
                .addAttributes("a", "target")
                .addProtocols("a", "href", "http", "https");
        String clean = Jsoup.clean(html, baseUri, safelist);
        Document fragment = Jsoup.parseBodyFragment(clean, baseUri);
        fragment.select("a").attr("rel", "noopener noreferrer nofollow");
        return fragment.body().html();
    }

    private String sanitizePlainText(String text) {
        return text == null ? null : Jsoup.clean(text, Safelist.none());
    }

    private LocalDateTime toLocalDateTime(Date date) {
        if (date == null) {
            return LocalDateTime.now();
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }
}
