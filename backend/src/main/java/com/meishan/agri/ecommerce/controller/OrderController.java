package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.dto.OrderCreateDTO;
import com.meishan.agri.ecommerce.dto.OrderDTO;
import com.meishan.agri.ecommerce.dto.ShipDTO;
import com.meishan.agri.ecommerce.entity.Orders;
import com.meishan.agri.ecommerce.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @SaCheckLogin
    @PostMapping("/orders")
    public Result<Orders> create(@RequestBody OrderCreateDTO dto) {
        return Result.ok(orderService.create(StpUtil.getLoginIdAsLong(), dto));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/pay")
    public Result<Orders> pay(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return Result.ok(orderService.pay(StpUtil.getLoginIdAsLong(), id, body.getOrDefault("channel", "MOCK_WECHAT")));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/cancel")
    public Result<Orders> cancel(@PathVariable Long id) {
        return Result.ok(orderService.cancel(StpUtil.getLoginIdAsLong(), id));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/confirm")
    public Result<Orders> confirm(@PathVariable Long id) {
        return Result.ok(orderService.confirm(StpUtil.getLoginIdAsLong(), id));
    }

    @SaCheckLogin
    @PostMapping("/orders/{id}/refund")
    public Result<Orders> refund(@PathVariable Long id) {
        return Result.ok(orderService.refund(StpUtil.getLoginIdAsLong(), id));
    }

    @SaCheckLogin
    @GetMapping("/orders")
    public Result<Page<Orders>> myOrders(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.pageByUser(StpUtil.getLoginIdAsLong(), page, size));
    }

    @SaCheckLogin
    @GetMapping("/orders/{id}")
    public Result<OrderDTO> myOrder(@PathVariable Long id) {
        return Result.ok(orderService.detail(StpUtil.getLoginIdAsLong(), id, false));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @GetMapping("/seller/orders")
    public Result<Page<Orders>> sellerOrders(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(orderService.pageBySeller(StpUtil.getLoginIdAsLong(), page, size));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @GetMapping("/seller/orders/{id}")
    public Result<OrderDTO> sellerOrder(@PathVariable Long id) {
        return Result.ok(orderService.detail(StpUtil.getLoginIdAsLong(), id, true));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/orders/{id}/ship")
    public Result<Orders> ship(@PathVariable Long id, @RequestBody ShipDTO dto) {
        return Result.ok(orderService.ship(StpUtil.getLoginIdAsLong(), id, dto));
    }
}
