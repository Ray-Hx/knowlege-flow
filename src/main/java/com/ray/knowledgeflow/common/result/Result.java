package com.ray.knowledgeflow.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果封装
 *
 * @author Ray Huang
 */
@Data
public class Result<T> implements Serializable {
    private Integer code;
    private String message;
    private T data;

    private Result() {
    }

    private Result(Integer code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success() {
        return new Result<>(200, "成功", null);
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "成功", data);
    }

    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200, message, data);
    }

    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }

    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }

    public static <T> Result<T> failed(String message) {
        return fail(message);
    }

    public static <T> Result<T> failed(Integer code, String message) {
        return fail(code, message);
    }
}
