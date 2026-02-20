package com.ray.knowledgeflow.module.content.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * 内容源 DTO
 *
 * @author Ray
 * @since 2024-01-16
 */
@Data
public class ContentSourceDto {

    /**
     * ID
     */
    private Long id;

    /**
     * 源类型: RSS, GITHUB, ARXIV, FILE
     */
    @NotBlank(message = "源类型不能为空")
    @Pattern(regexp = "RSS|GITHUB|ARXIV|FILE", message = "源类型必须是 RSS、GITHUB、ARXIV 或 FILE")
    private String type;

    /**
     * 源名称
     */
    @NotBlank(message = "源名称不能为空")
    @Size(min = 1, max = 128, message = "源名称长度必须在1-128个字符之间")
    private String name;

    /**
     * 源 URL
     */
    @NotBlank(message = "源URL不能为空")
    @Size(max = 512, message = "URL长度不能超过512个字符")
    private String url;

    /**
     * 同步间隔 (分钟)
     */
    @Min(value = 1, message = "同步间隔至少为1分钟")
    @Max(value = 10080, message = "同步间隔不能超过7天")
    private Integer syncInterval;

    /**
     * 状态: 1-启用, 0-禁用
     */
    private Integer status;
}
