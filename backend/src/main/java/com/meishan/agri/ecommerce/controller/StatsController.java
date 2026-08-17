package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class StatsController {
    private final StatsService statsService;

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @GetMapping("/api/seller/stats")
    public Result<Map<String, Object>> sellerStats() {
        return Result.ok(statsService.sellerStats(StpUtil.getLoginIdAsLong()));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @GetMapping("/api/admin/stats")
    public Result<Map<String, Object>> adminStats() {
        return Result.ok(statsService.adminStats());
    }
}
