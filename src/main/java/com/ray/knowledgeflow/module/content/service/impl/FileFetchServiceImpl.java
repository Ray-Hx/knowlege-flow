package com.ray.knowledgeflow.module.content.service.impl;

import com.ray.knowledgeflow.common.exception.BusinessException;
import com.ray.knowledgeflow.common.utils.MinioUtil;
import com.ray.knowledgeflow.module.content.entity.ContentItem;
import com.ray.knowledgeflow.module.content.mapper.ContentItemMapper;
import com.ray.knowledgeflow.module.content.service.IFileFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 文件抓取服务实现类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileFetchServiceImpl implements IFileFetchService {

    private final MinioUtil minioUtil;
    private final ContentItemMapper contentItemMapper;
    private final ApplicationEventPublisher eventPublisher;

    private static final List<String> ALLOWED_TYPES = Arrays.asList(
            "application/pdf",
            "text/markdown",
            "text/html",
            "text/plain",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Override
    public Long processUploadedFile(MultipartFile file, Long userId) {
        try {
            // 验证文件
            validate(file);

            // 提取文本内容
            String content = extractText(file);

            // 创建内容项
            ContentItem item = new ContentItem();
            item.setUserId(userId);
            item.setSourceId(null); // 手动上传，无source_id
            item.setTitle(file.getOriginalFilename());
            item.setUrl("file://" + file.getOriginalFilename());
            item.setContentText(content.length() > 10000 ? content.substring(0, 10000) : content);
            item.setStatus(0); // 未读
            item.setPublishTime(LocalDateTime.now());

            // 生成摘要（取前500字符）
            String summary = content.length() > 500 ? content.substring(0, 500) + "..." : content;
            item.setSummary(summary);

            contentItemMapper.insert(item);
            eventPublisher.publishEvent(new com.ray.knowledgeflow.module.search.event.ContentSavedEvent(this, item));

            log.info("文件上传处理完成: file={}, userId={}, itemId={}",
                    file.getOriginalFilename(), userId, item.getId());

            return item.getId();

        } catch (Exception e) {
            log.error("文件处理失败: file={}", file.getOriginalFilename(), e);
            throw new BusinessException(500, "文件处理失败: " + e.getMessage());
        }
    }

    /**
     * 验证文件
     */
    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "文件不能为空");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException(400, "文件大小不能超过10MB");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new BusinessException(400, "不支持的文件类型，仅支持PDF、Markdown、HTML、TXT");
        }
    }

    /**
     * 使用Apache Tika提取文本
     */
    private String extractText(MultipartFile file) throws Exception {
        Tika tika = new Tika();
        String content = tika.parseToString(file.getInputStream());

        if (content == null || content.trim().isEmpty()) {
            throw new BusinessException(400, "无法提取文件内容");
        }

        return content.trim();
    }
}
