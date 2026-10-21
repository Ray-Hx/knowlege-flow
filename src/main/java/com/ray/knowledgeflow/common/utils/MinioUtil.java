package com.ray.knowledgeflow.common.utils;

import com.ray.knowledgeflow.common.exception.BusinessException;
import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO 工具类
 *
 * @author Ray
 * @since 2024-01-16
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucket-name:knowledge-flow}")
    private String bucketName;

    @Value("${minio.endpoint:http://localhost:9000}")
    private String endpoint;

    /**
     * 上传文件
     *
     * @param file   文件
     * @param folder 文件夹
     * @return 文件访问 URL
     */
    public String uploadFile(MultipartFile file, String folder) {
        return uploadImage(file, folder, 10 * 1024 * 1024L);
    }

    public String uploadImage(MultipartFile file, String folder, long maxBytes) {
        ImageUploadValidator.ImageMetadata metadata = ImageUploadValidator.validate(file, maxBytes);

        // 确保桶存在
        ensureBucketExists();

        // 后缀和 Content-Type 均来自服务端检测，不信任用户提交的文件名/MIME。
        String objectName = folder + "/" + UUID.randomUUID() + metadata.extension();

        try (InputStream inputStream = file.getInputStream()) {
            return uploadFile(inputStream, objectName, file.getSize(), metadata.contentType());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(500, "文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 上传文件 (通用方法)
     *
     * @param inputStream 输入流
     * @param objectName  对象名称 (完整路径)
     * @param size        文件大小
     * @param contentType 内容类型
     * @return 文件访问 URL
     */
    public String uploadFile(InputStream inputStream, String objectName, long size, String contentType) {
        // 确保桶存在
        ensureBucketExists();

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(inputStream, size, -1)
                            .contentType(contentType)
                            .build());
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(500, "文件上传失败：" + e.getMessage());
        }

        return endpoint + "/" + bucketName + "/" + objectName;
    }

    /**
     * 删除文件
     *
     * @param objectName 对象名称
     */
    public void deleteFile(String objectName) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
        } catch (Exception e) {
            log.error("文件删除失败", e);
            throw new BusinessException(500, "文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 确保桶存在
     */
    private void ensureBucketExists() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("创建 MinIO 桶: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("检查/创建桶失败", e);
            throw new BusinessException(500, "存储初始化失败");
        }
    }
}
