package com.edu.schedule.common;

import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e, HttpServletResponse response) {
        // 鉴权相关异常映射到真实 HTTP 状态码，方便网关/前端拦截器区分
        Integer code = e.getCode();
        if (ResultCode.UNAUTHORIZED.getCode().equals(code)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED); // 401
            log.warn("鉴权失败(401): {}", e.getMessage());
        } else if (ResultCode.FORBIDDEN.getCode().equals(code)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);    // 403
            log.warn("权限不足(403): {}", e.getMessage());
        } else if (ResultCode.TOO_MANY_REQUESTS.getCode().equals(code)) {
            response.setStatus(429);                                  // HTTP 429 Too Many Requests
            log.warn("限流拦截(429): {}", e.getMessage());
        } else {
            response.setStatus(HttpServletResponse.SC_OK);           // 其他业务异常仍然 200，前端通过 body.code 判断
            log.warn("业务异常: {}", e.getMessage());
        }
        return Result.fail(code, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(";"));
        return Result.fail(ResultCode.PARAM_ERROR.getCode(), msg);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.fail(ResultCode.FAIL.getCode(), "系统异常: " + e.getMessage());
    }
}