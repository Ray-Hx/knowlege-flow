package com.ray.knowledgeflow.module.user.dto.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发送私信请求 DTO
 */
@Data
public class SendMessageDto {

    @NotNull(message = "接收方ID不能为空")
    @Min(value = 1, message = "接收方ID不合法")
    private Long receiverId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 2000, message = "消息内容不能超过2000个字符")
    private String content;
}
