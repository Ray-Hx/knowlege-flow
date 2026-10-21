package com.ray.knowledgeflow.common.exception;

import com.ray.knowledgeflow.common.result.Result;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Set;

/**
 * 全局异常处理器
 *
 * @author Ray
 * @since 2024-01-14
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<String>> handleBusinessException(BusinessException e) {
        log.error("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null || !status.isError()) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(Result.fail(e.getCode(), e.getMessage()));
    }

    /**
     * 处理认证异常 (Spring Security)
     */
    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public Result<String> handleAuthenticationException(AuthenticationException e) {
        log.error("认证异常: {}", e.getMessage());
        return Result.fail(401, "用户名或密码错误");
    }

    /**
     * 处理参数校验异常 (@RequestBody 参数校验)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleValidationException(MethodArgumentNotValidException e) {
        BindingResult bindingResult = e.getBindingResult();
        FieldError fieldError = bindingResult.getFieldError();

        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";
        log.warn("参数校验失败: field={}, message={}",
                fieldError != null ? fieldError.getField() : "unknown", msg);

        return Result.fail(400, msg);
    }

    /**
     * 处理参数校验异常 (表单绑定异常)
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleBindException(BindException e) {
        FieldError fieldError = e.getFieldError();

        String msg = fieldError != null ? fieldError.getDefaultMessage() : "参数绑定失败";
        log.warn("参数绑定失败: field={}, message={}",
                fieldError != null ? fieldError.getField() : "unknown", msg);

        return Result.fail(400, msg);
    }

    /**
     * 处理单参数校验异常 (如 @PathVariable @Min(1) Long id)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Result<String> handleConstraintViolationException(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> violations = e.getConstraintViolations();

        String msg = violations.isEmpty() ? "参数校验失败"
                : violations.iterator().next().getMessage();
        log.warn("参数校验失败: {}", msg);

        return Result.fail(400, msg);
    }

    /**
     * 处理系统异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<String> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(500, "系统繁忙，请稍后重试");
    }
}
