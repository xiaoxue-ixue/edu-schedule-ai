package com.edu.schedule.config;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.ResultCode;
import com.edu.schedule.utils.JwtUtils;
import com.edu.schedule.utils.UserContext;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 鉴权 + 角色权限拦截器
 *
 * 权限矩阵（最小权限原则）：
 *  - 公开（无需登录）：由 WebMvcConfig.excludePathPatterns 管理（/api/auth/login, /api/auth/register）
 *  - 所有登录用户：/api/auth/info、/api/auth/password（个人信息 / 改密码）
 *  - student 角色：/api/student/*（学生端个人中心）
 *  - teacher 角色：/api/teacher-portal/*（教师端个人中心）
 *  - admin 角色：其他所有 /api/*（教务端管理接口，含用户/课程/班级/排课/AI 配置等）
 *
 * 匹配策略：使用 requestURI.startsWith 精确匹配前缀，避免 contains 误命中
 */
@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    // 所有已登录用户都可访问的路径（改密码、查个人信息）
    private static final String[] COMMON_PATHS = {
            "/api/auth/info",
            "/api/auth/password"
    };

    // student 角色专属前缀
    private static final String STUDENT_PREFIX = "/api/student/";

    // teacher 角色专属前缀
    private static final String TEACHER_PREFIX = "/api/teacher-portal/";

    // 角色常量
    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_STUDENT = "student";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // ====== 第一步：JWT 登录态校验（原有逻辑保持不变）======
        String header = request.getHeader("Authorization");
        String token = null;
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        }
        if (token == null || token.isBlank()) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Claims claims = jwtUtils.parse(token);
        if (claims == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        Long userId = claims.get("userId", Long.class);
        if (userId == null) {
            userId = Long.valueOf(claims.getSubject());
        }
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);
        if (role == null || role.isBlank()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号缺少角色信息，请联系管理员");
        }
        UserContext.set(new UserContext.LoginUser(userId, username, role));

        // ====== 第二步：基于路径前缀 + 角色的权限校验 ======
        String uri = request.getRequestURI();
        // 去除 context-path（本项目 context-path 是 "/"，保险起见处理）
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }

        // 1) 通用接口（改密/个人信息）：所有登录用户均可
        for (String p : COMMON_PATHS) {
            if (uri.startsWith(p)) {
                return true;
            }
        }

        // 2) student 只能访问 /api/student/*
        if (ROLE_STUDENT.equalsIgnoreCase(role)) {
            if (uri.startsWith(STUDENT_PREFIX)) {
                return true;
            }
            throw new BusinessException(ResultCode.FORBIDDEN, "学生账号无权访问该资源");
        }

        // 3) teacher 只能访问 /api/teacher-portal/*
        if (ROLE_TEACHER.equalsIgnoreCase(role)) {
            if (uri.startsWith(TEACHER_PREFIX)) {
                return true;
            }
            throw new BusinessException(ResultCode.FORBIDDEN, "教师账号无权访问该资源");
        }

        // 4) admin 可以访问所有其他管理接口
        if (ROLE_ADMIN.equalsIgnoreCase(role)) {
            return true;
        }

        // 5) 未知角色，一律拒绝
        throw new BusinessException(ResultCode.FORBIDDEN, "未知角色：" + role);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}