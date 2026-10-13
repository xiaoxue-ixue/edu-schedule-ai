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

@Component
public class JwtAuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtils jwtUtils;

    // 所有已登录用户都可访问的路径
    private static final String[] COMMON_PATHS = {
            "/api/auth/info",
            "/api/auth/password"
    };

    // 所有已登录用户都可 GET 的只读接口（公共数据查询）
    private static final String[] COMMON_READ_PATHS = {
            "/api/announcement",
            "/api/term",
            "/api/time-slot"
    };

    // teacher 角色专属前缀
    private static final String TEACHER_PREFIX = "/api/teacher-portal/";
    // teacher 专属：可用时间管理（带后端数据隔离）
    private static final String TEACHER_TIME_PREFIX = "/api/teacher-available-time";

    // student 角色专属前缀
    private static final String STUDENT_PREFIX = "/api/student/";

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_TEACHER = "teacher";
    private static final String ROLE_STUDENT = "student";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
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

        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        String method = request.getMethod().toUpperCase();

        // 1) 通用接口（改密/个人信息）
        for (String p : COMMON_PATHS) {
            if (uri.startsWith(p)) {
                return true;
            }
        }

        // 2) 公共只读接口（所有登录用户可 GET）
        if ("GET".equals(method)) {
            for (String p : COMMON_READ_PATHS) {
                if (uri.startsWith(p)) {
                    return true;
                }
            }
        }

        // 3) student 角色：只能访问 /api/student/*
        if (ROLE_STUDENT.equalsIgnoreCase(role)) {
            if (uri.startsWith(STUDENT_PREFIX)) {
                return true;
            }
            throw new BusinessException(ResultCode.FORBIDDEN, "学生账号无权访问该资源");
        }

        // 4) teacher 角色：可访问 /api/teacher-portal/* + /api/teacher-available-time/*
        if (ROLE_TEACHER.equalsIgnoreCase(role)) {
            if (uri.startsWith(TEACHER_PREFIX)) {
                return true;
            }
            if (uri.startsWith(TEACHER_TIME_PREFIX)) {
                return true;
            }
            throw new BusinessException(ResultCode.FORBIDDEN, "教师账号无权访问该资源");
        }

        // 5) admin 可访问所有管理接口
        if (ROLE_ADMIN.equalsIgnoreCase(role)) {
            return true;
        }

        throw new BusinessException(ResultCode.FORBIDDEN, "未知角色：" + role);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}