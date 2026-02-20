package com.ray.knowledgeflow.module.knowledge.service;

import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 链接解析服务
 *
 * @author Ray
 * @since 2024-01-21
 */
@Service
public class LinkParserService {

    /**
     * Wiki 链接正则: [[Title]] 或 [[Title|Alias]]
     */
    private static final Pattern WIKI_LINK_PATTERN = Pattern.compile("\\[\\[(.*?)(?:\\|(.*?))?\\]\\]");

    /**
     * 提取 markdown 中的 wiki 链接标题
     *
     * @param content Markdown 内容
     * @return 链接标题集合
     */
    public Set<String> extractWikiLinks(String content) {
        Set<String> links = new HashSet<>();
        if (content == null || content.isEmpty()) {
            return links;
        }

        Matcher matcher = WIKI_LINK_PATTERN.matcher(content);
        while (matcher.find()) {
            // Group 1 是标题, Group 2 是别名 (如果存在)
            String title = matcher.group(1).trim();
            if (!title.isEmpty()) {
                links.add(title);
            }
        }
        return links;
    }
}
