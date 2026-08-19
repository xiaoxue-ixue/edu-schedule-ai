package com.edu.schedule.controller;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.Result;
import com.edu.schedule.common.ResultCode;
import com.edu.schedule.config.AuthProperties;
import com.edu.schedule.entity.User;
import com.edu.schedule.service.AuthService;
import com.edu.schedule.service.CaptchaService;
import com.edu.schedule.service.RateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginDTO dto) {
        return Result.success(authService.login(dto.getUsername(), dto.getPassword()));
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO dto) {
        // ---------- 第 1 层：注册总开关 ----------
        if (!authProperties.isRegistrationEnabled()) {
            throw new BusinessException(ResultCode.REGISTRATION_CLOSED,
                    "管理员已关闭开放注册，请联系管理员创建账号（默认 admin/admin）");
        }
        // ---------- 第 2 层：图形验证码（一次性，防止简单脚本爆破）----------
        if (!captchaService.validateAndConsume(dto.getCaptchaKey(), dto.getCaptchaCode())) {
            throw new BusinessException(ResultCode.CAPTCHA_INVALID,
                    "验证码错误、已过期或已使用，请点击刷新重新输入");
        }
        // ---------- 第 3 层：双维度限流（防真人疯狂点 + 注册机批量）----------
        rateLimitService.checkAndCountRegister(httpServletRequest, dto.getUsername());
        // ---------- 全部通过 → 写入用户 ----------
        authService.register(dto.getUsername(), dto.getPassword(), dto.getRealName());
        return Result.success();
    }

    @GetMapping("/info")
    public Result<User> info() {
        return Result.success(authService.currentUser());
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody ChangePwdDTO dto) {
        authService.changePassword(dto.getOldPassword(), dto.getNewPassword());
        return Result.success();
    }

    @Data
    public static class LoginDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }

    @Data
    public static class RegisterDTO {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
        @NotBlank(message = "姓名不能为空")
        private String realName;
        /** 图形验证码 key（由 /api/captcha/generate 返回） */
        @NotBlank(message = "请先点击获取验证码")
        private String captchaKey;
        /** 图形验证码（用户输入的 4 位字符，忽略大小写） */
        @NotBlank(message = "请输入图形验证码")
        private String captchaCode;
    }

    @Data
    public static class ChangePwdDTO {
        @NotBlank(message = "原密码不能为空")
        private String oldPassword;
        @NotBlank(message = "新密码不能为空")
        private String newPassword;
    }
}
