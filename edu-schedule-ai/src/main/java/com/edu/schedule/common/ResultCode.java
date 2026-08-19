package com.edu.schedule.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 响应码枚举
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    FAIL(500, "操作失败"),
    /** 限流：429 Too Many Requests（网关/前端拦截器可识别真实 HTTP 状态） */
    TOO_MANY_REQUESTS(429, "请求过于频繁，请稍后再试"),
    /** 验证码错误/过期：业务级 40001（不会与 PARAM_ERROR 混，方便前端定位） */
    CAPTCHA_INVALID(40001, "验证码错误或已过期，请刷新"),
    /** 注册关闭 */
    REGISTRATION_CLOSED(4031, "管理员已关闭开放注册，请联系管理员创建账号");

    private final Integer code;
    private final String message;
}
