package com.meishan.agri.system.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.common.Result;
import com.meishan.agri.system.dto.SellerApplyDTO;
import com.meishan.agri.system.entity.SellerInfo;
import com.meishan.agri.system.service.SellerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class SellerController {
    private final SellerService sellerService;

    @SaCheckLogin
    @PostMapping("/api/seller/apply")
    public Result<SellerInfo> apply(@RequestBody SellerApplyDTO dto) {
        return Result.ok(sellerService.apply(StpUtil.getLoginIdAsLong(), dto));
    }

    @SaCheckLogin
    @GetMapping("/api/seller/info")
    public Result<SellerInfo> myInfo() {
        return Result.ok(sellerService.myInfo(StpUtil.getLoginIdAsLong()));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @GetMapping("/api/admin/sellers")
    public Result<List<SellerInfo>> list(@RequestParam(required = false) String status) {
        return Result.ok(sellerService.listByStatus(status));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PostMapping("/api/admin/sellers/{id}/approve")
    public Result<Void> approve(@PathVariable Long id) {
        sellerService.approve(id);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PostMapping("/api/admin/sellers/{id}/reject")
    public Result<Void> reject(@PathVariable Long id) {
        sellerService.reject(id);
        return Result.ok(null);
    }
}
