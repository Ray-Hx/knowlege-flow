package com.ray.knowledgeflow.module.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ray.knowledgeflow.common.utils.HttpClientUtil;
import com.ray.knowledgeflow.common.utils.OutboundUrlValidator;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.entity.ContentSource;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.mapper.ContentSourceMapper;
import com.ray.knowledgeflow.module.content.service.IGitHubFetchService;
import com.ray.knowledgeflow.module.user.entity.SysUserProfile;
import com.ray.knowledgeflow.module.user.mapper.UserProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * GitHub抓取服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GitHubFetchServiceImpl implements IGitHubFetchService {

    private final ContentItemMapper contentItemMapper;
    private final ContentSourceMapper contentSourceMapper;
    private final UserProfileMapper userProfileMapper;
    private final org.springframework.context.ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    public void fetchAsync(ContentSource source) {
        try {
            int count = fetch(source);
            log.info("GitHub 抓取完成: source={}, newItems={}", source.getName(), count);
        } catch (Exception e) {
            log.error("GitHub 抓取失败: source={}", source.getName(), e);
        }
    }

    @Override
    public int fetch(ContentSource source) {
        int newCount = 0;

        try {
            // 获取用户的GitHub Token
            SysUserProfile profile = userProfileMapper.selectById(source.getUserId());
            String githubToken = profile != null ? profile.getGithubToken() : null;

            if (githubToken == null || githubToken.isEmpty()) {
                log.warn("用户未配置GitHub Token, userId={}", source.getUserId());
                throw new RuntimeException("请先在个人设置中配置GitHub Personal Access Token");
            }

            // 准备请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Accept", "application/vnd.github.v3+json");
            headers.put("Authorization", "Bearer " + githubToken);

            // 获取starred仓库列表
            String apiUrl = source.getUrl();
            if (apiUrl == null || apiUrl.isEmpty()) {
                apiUrl = "https://api.github.com/user/starred";
            }
            // 只有 GitHub 官方 API 才能接收用户的 Personal Access Token。
            OutboundUrlValidator.requireHost(apiUrl, true, Set.of("api.github.com"));

            // 添加分页参数
            if (!apiUrl.contains("?")) {
                apiUrl += "?per_page=100";
            }

            log.info("开始抓取GitHub starred仓库: url={}", apiUrl);

            String response = HttpClientUtil.get(apiUrl, headers);

            // 解析JSON响应
            JsonNode repos = objectMapper.readTree(response);

            for (JsonNode repo : repos) {
                String repoUrl = repo.get("html_url").asText();

                // 检查是否已存在
                long exists = contentItemMapper.selectCount(new LambdaQueryWrapper<ContentItem>()
                        .eq(ContentItem::getUserId, source.getUserId())
                        .eq(ContentItem::getUrl, repoUrl));

                if (exists > 0) {
                    continue;
                }

                // 创建内容项
                ContentItem item = new ContentItem();
                item.setSourceId(source.getId());
                item.setUserId(source.getUserId());
                item.setTitle(repo.get("full_name").asText());
                item.setUrl(repoUrl);
                item.setAuthor(repo.get("owner").get("login").asText());
                item.setStatus(0); // 未读

                // 设置发布时间
                String createdAt = repo.get("created_at").asText();
                item.setPublishTime(parseGitHubDate(createdAt));

                // 设置摘要（仓库描述）
                if (repo.has("description") && !repo.get("description").isNull()) {
                    String description = repo.get("description").asText();
                    item.setSummary(description.length() > 500 ? description.substring(0, 500) + "..." : description);
                }

                // 获取README内容
                try {
                    String readmeUrl = "https://api.github.com/repos/" + repo.get("full_name").asText() + "/readme";
                    String readmeResponse = HttpClientUtil.get(readmeUrl, headers);
                    JsonNode readmeNode = objectMapper.readTree(readmeResponse);

                    if (readmeNode.has("content")) {
                        String base64Content = readmeNode.get("content").asText().replace("\n", "");
                        String readmeContent = new String(Base64.getDecoder().decode(base64Content));

                        // 转换Markdown为纯文本
                        String plainText = Jsoup.clean(Jsoup.parse(readmeContent).text(), Safelist.none());
                        item.setContentText(
                                plainText.length() > 5000 ? plainText.substring(0, 5000) + "..." : plainText);
                    }
                } catch (Exception e) {
                    log.debug("无法获取README: repo={}", repo.get("full_name").asText());
                }

                // 构建标签
                StringBuilder tags = new StringBuilder("[");
                if (repo.has("language") && !repo.get("language").isNull()) {
                    tags.append("\"").append(repo.get("language").asText()).append("\"");
                }
                if (repo.has("topics") && repo.get("topics").isArray() && repo.get("topics").size() > 0) {
                    for (int i = 0; i < Math.min(5, repo.get("topics").size()); i++) {
                        if (tags.length() > 1)
                            tags.append(",");
                        tags.append("\"").append(repo.get("topics").get(i).asText()).append("\"");
                    }
                }
                tags.append("]");
                item.setTags(tags.toString());

                contentItemMapper.insert(item);
                eventPublisher
                        .publishEvent(new com.ray.knowledgeflow.module.search.event.ContentSavedEvent(this, item));
                newCount++;

                // 避免触发GitHub API速率限制，添加短暂延迟
                Thread.sleep(100);
            }

            log.info("GitHub抓取完成: 新增{}个仓库", newCount);

        } catch (Exception e) {
            log.error("解析GitHub数据失败", e);
            throw new RuntimeException("GitHub抓取失败: " + e.getMessage());
        }

        return newCount;
    }

    /**
     * 解析GitHub日期格式
     */
    private LocalDateTime parseGitHubDate(String dateStr) {
        try {
            ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
            return zonedDateTime.toLocalDateTime();
        } catch (Exception e) {
            log.warn("日期解析失败: {}", dateStr);
            return LocalDateTime.now();
        }
    }

    @Override
    public void fetchFromSource(Long sourceId, Long userId) {
        ContentSource source = contentSourceMapper.selectOne(new LambdaQueryWrapper<ContentSource>()
                .eq(ContentSource::getId, sourceId)
                .eq(ContentSource::getUserId, userId));
        if (source == null || source.getType() == null || !"GITHUB".equalsIgnoreCase(source.getType())) {
            throw new IllegalArgumentException("内容源不存在、无权访问或类型不匹配");
        }
        fetch(source);
    }
}
