package com.ray.knowledgeflow.module.content.controller;

import com.ray.knowledgeflow.common.result.Result;
import com.ray.knowledgeflow.module.content.service.IFileFetchService;
import com.ray.knowledgeflow.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器
 *
 * @author Ray
 * @since 2024-01-16
 */
@Tag(name = "文件上传管理")
@RestController
@RequestMapping("/content/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final IFileFetchService fileFetchService;

    /**
     * 上传文件并解析内容
     */
    @PostMapping
    @Operation(summary = "上传文件", description = "支持PDF、Markdown、HTML、TXT文件，自动提取文本内容")
    public Result<Long> uploadFile(@RequestParam("file") MultipartFile file) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long itemId = fileFetchService.processUploadedFile(file, userId);
        return Result.success(itemId);
    }
}
