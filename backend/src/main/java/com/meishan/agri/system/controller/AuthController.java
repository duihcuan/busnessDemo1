package com.meishan.agri.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.common.Result;
import com.meishan.agri.system.dto.AdminLoginRequest;
import com.meishan.agri.system.dto.LoginRequest;
import com.meishan.agri.system.dto.LoginResponse;
import com.meishan.agri.system.dto.WechatLoginRequest;
import com.meishan.agri.system.entity.User;
import com.meishan.agri.system.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @GetMapping("/api/ping")
    public Result<String> ping() { return Result.ok("pong"); }

    @PostMapping("/api/auth/mock-login")
    public Result<LoginResponse> mockLogin(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.mockLogin(req));
    }

    @PostMapping("/api/auth/wechat-login")
    public Result<LoginResponse> wechatLogin(@RequestBody WechatLoginRequest req) {
        return Result.ok(authService.wechatLogin(req));
    }

    @PostMapping("/api/auth/admin-login")
    public Result<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest req) {
        return Result.ok(authService.adminLogin(req));
    }

    @PostMapping("/api/auth/logout")
    public Result<Void> logout() { StpUtil.logout(); return Result.ok(null); }

    @SaCheckLogin
    @GetMapping("/api/auth/info")
    public Result<User> info() { return Result.ok(authService.currentUser()); }
}
