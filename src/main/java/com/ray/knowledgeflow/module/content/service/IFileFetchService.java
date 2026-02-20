package com.ray.knowledgeflow.module.content.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件抓取服务接口
 *
 * @author Ray
 * @since 2024-01-16
 */
public interface IFileFetchService {

    /**
     * 处理上传的文件
     *
     * @param file   上传的文件
     * @param userId 用户ID
     * @return 内容项ID
     */
    Long processUploadedFile(MultipartFile file, Long userId);
}
