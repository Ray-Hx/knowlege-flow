package com.ray.knowledgeflow.module.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hankcs.hanlp.HanLP;
import com.ray.knowledgeflow.common.utils.SimHashUtil;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 内容处理服务
 * 提供文本去重、关键词提取、自动分类、AI摘要生成功能
 *
 * @author Ray
 * @since 2024-01-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentProcessorService {

    private final ContentItemMapper contentItemMapper;
    private final ChatLanguageModel chatLanguageModel;

    /**
     * SimHash 汉明距离阈值（小于此值视为重复）
     */
    private static final int DUPLICATE_THRESHOLD = 3;

    /**
     * 关键词提取数量
     */
    private static final int KEYWORD_COUNT = 10;

    /**
     * 分类规则（关键词 -> 分类）
     */
    private static final Map<String, Set<String>> CATEGORY_RULES = new HashMap<>();

    static {
        // 编程开发
        CATEGORY_RULES.put("编程开发", new HashSet<>(Arrays.asList(
                "java", "python", "编程", "算法", "代码", "框架", "开发", "golang", "rust",
                "spring", "mybatis", "数据结构", "设计模式", "api")));

        // 机器学习/AI
        CATEGORY_RULES.put("机器学习", new HashSet<>(Arrays.asList(
                "深度学习", "神经网络", "机器学习", "ai", "人工智能", "模型", "训练",
                "tensorflow", "pytorch", "数据科学", "nlp", "cv", "计算机视觉")));

        // 前端技术
        CATEGORY_RULES.put("前端技术", new HashSet<>(Arrays.asList(
                "vue", "react", "javascript", "css", "前端", "html", "typescript",
                "angular", "webpack", "ui", "ux", "node")));

        // 数据库
        CATEGORY_RULES.put("数据库", new HashSet<>(Arrays.asList(
                "mysql", "redis", "数据库", "sql", "nosql", "mongodb", "postgresql",
                "elasticsearch", "数据存储", "orm")));

        // 运维/DevOps
        CATEGORY_RULES.put("运维DevOps", new HashSet<>(Arrays.asList(
                "docker", "kubernetes", "devops", "运维", "ci/cd", "jenkins",
                "云计算", "linux", "k8s", "容器")));

        // 架构设计
        CATEGORY_RULES.put("架构设计", new HashSet<>(Arrays.asList(
                "架构", "微服务", "分布式", "系统设计", "高并发", "高可用",
                "中间件", "消息队列", "缓存", "负载均衡")));
    }

    /**
     * 综合处理内容
     *
     * @param item 内容项
     */
    public void processContent(ContentItem item) {
        if (item == null || item.getContentText() == null || item.getContentText().isEmpty()) {
            log.warn("内容为空，跳过处理: id={}", item != null ? item.getId() : "null");
            return;
        }

        try {
            // 1. 去重检测
            if (isDuplicate(item)) {
                log.info("检测到重复内容: id={}, title={}", item.getId(), item.getTitle());
                item.setStatus(ContentItem.STATUS_DUPLICATE);
                // 继续提取标签和分类，但可能跳过AI摘要以节省成本
            }

            // 2. 关键词提取
            List<String> keywords = extractKeywords(item.getContentText());
            if (!keywords.isEmpty()) {
                // 将关键词保存到 tags 字段（JSON 格式）
                String tagsJson = keywords.stream()
                        .map(k -> "\"" + k + "\"")
                        .collect(Collectors.joining(",", "[", "]"));
                item.setTags(tagsJson);
                log.debug("提取关键词: id={}, keywords={}", item.getId(), keywords);
            }

            // 3. 自动分类
            String category = autoClassify(item);
            log.debug("自动分类: id={}, category={}", item.getId(), category);

            // 4. AI 摘要生成 (非重复内容且摘要为空时)
            if (item.getStatus() != ContentItem.STATUS_DUPLICATE && !StringUtils.hasText(item.getSummary())) {
                try {
                    String summary = generateSummary(item.getContentText());
                    item.setSummary(summary);
                    log.info("AI摘要生成成功: id={}", item.getId());
                } catch (Exception e) {
                    log.error("AI摘要生成失败: id={}", item.getId(), e);
                }
            }

            // 更新数据库
            contentItemMapper.updateById(item);
            log.info("内容处理完成: id={}, category={}, status={}",
                    item.getId(), category, item.getStatus());

        } catch (Exception e) {
            log.error("内容处理失败: id={}", item.getId(), e);
        }
    }

    /**
     * 检测是否为重复内容
     *
     * @param item 内容项
     * @return true-重复，false-不重复
     */
    public boolean isDuplicate(ContentItem item) {
        if (item.getContentText() == null || item.getContentText().isEmpty()) {
            return false;
        }

        try {
            // 计算当前内容的 SimHash
            long currentHash = SimHashUtil.simHash(item.getContentText());

            // 查询同一用户的其他内容 (排除自身和已标记为重复的)
            List<ContentItem> existingItems = contentItemMapper.selectList(
                    new LambdaQueryWrapper<ContentItem>()
                            .eq(ContentItem::getUserId, item.getUserId())
                            .ne(ContentItem::getId, item.getId())
                            .ne(ContentItem::getStatus, ContentItem.STATUS_DUPLICATE) // 不与已重复的比较
                            .last("LIMIT 100") // 只比较最近100条
            );

            // 与已有内容比较
            for (ContentItem existing : existingItems) {
                if (existing.getContentText() == null || existing.getContentText().isEmpty()) {
                    continue;
                }

                long existingHash = SimHashUtil.simHash(existing.getContentText());
                int distance = SimHashUtil.hammingDistance(currentHash, existingHash);

                if (distance < DUPLICATE_THRESHOLD) {
                    log.info("发现相似内容: newId={}, existingId={}, distance={}",
                            item.getId(), existing.getId(), distance);
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("去重检测失败: id={}", item.getId(), e);
            return false;
        }
    }

    /**
     * 提取关键词
     *
     * @param text 文本内容
     * @return 关键词列表
     */
    public List<String> extractKeywords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // 使用 HanLP 提取关键词
            List<String> keywords = HanLP.extractKeyword(text, KEYWORD_COUNT);
            return keywords != null ? keywords : Collections.emptyList();

        } catch (Exception e) {
            log.error("关键词提取失败", e);
            return Collections.emptyList();
        }
    }

    /**
     * 自动分类
     *
     * @param item 内容项
     * @return 分类名称
     */
    public String autoClassify(ContentItem item) {
        // 综合标题和内容进行分类
        String combinedText = (item.getTitle() + " " +
                (item.getSummary() != null ? item.getSummary() : "") + " " +
                (item.getContentText() != null ? item.getContentText().substring(0,
                        Math.min(500, item.getContentText().length())) : ""))
                .toLowerCase();

        // 统计每个分类的匹配关键词数量
        Map<String, Integer> categoryScores = new HashMap<>();

        for (Map.Entry<String, Set<String>> entry : CATEGORY_RULES.entrySet()) {
            String category = entry.getKey();
            Set<String> keywords = entry.getValue();

            int score = 0;
            for (String keyword : keywords) {
                if (combinedText.contains(keyword.toLowerCase())) {
                    score++;
                }
            }

            if (score > 0) {
                categoryScores.put(category, score);
            }
        }

        // 选择得分最高的分类
        if (categoryScores.isEmpty()) {
            return "其他";
        }

        return categoryScores.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("其他");
    }

    /**
     * 使用 AI 生成摘要
     *
     * @param text 正文内容
     * @return 简短摘要
     */
    public String generateSummary(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        // 截取前 2000 个字符进行摘要，避免 token 过长
        String input = text.substring(0, Math.min(text.length(), 2000));
        String prompt = "请为以下技术文章生成一个简短的摘要（200字以内），概括核心观点：\n\n" + input;

        return chatLanguageModel.generate(prompt);
    }
}
