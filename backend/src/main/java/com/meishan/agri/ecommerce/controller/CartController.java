package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.dto.CartDTO;
import com.meishan.agri.ecommerce.entity.CartItem;
import com.meishan.agri.ecommerce.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;

    @SaCheckLogin
    @GetMapping
    public Result<List<CartDTO>> list() {
        return Result.ok(cartService.list(StpUtil.getLoginIdAsLong()));
    }

    @SaCheckLogin
    @PostMapping
    public Result<CartItem> add(@RequestBody Map<String, Object> body) {
        Long productId = ((Number) body.get("productId")).longValue();
        Integer quantity = ((Number) body.get("quantity")).intValue();
        return Result.ok(cartService.add(StpUtil.getLoginIdAsLong(), productId, quantity));
    }

    @SaCheckLogin
    @PutMapping("/{id}")
    public Result<Void> updateQuantity(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        cartService.updateQuantity(StpUtil.getLoginIdAsLong(), id, ((Number) body.get("quantity")).intValue());
        return Result.ok(null);
    }

    @SaCheckLogin
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        cartService.remove(StpUtil.getLoginIdAsLong(), id);
        return Result.ok(null);
    }
}
