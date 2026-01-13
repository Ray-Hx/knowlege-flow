package com.ray.knowledgeflow.common.exception;

/**
 * 业务异常基类
 *
 * @author Ray Huang
 */
public class BusinessException extends RuntimeException {
    private Integer code;

    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }
}
