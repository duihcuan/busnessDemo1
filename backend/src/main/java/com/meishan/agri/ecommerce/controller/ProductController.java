package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.dto.ProductDTO;
import com.meishan.agri.ecommerce.entity.Product;
import com.meishan.agri.ecommerce.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    @GetMapping("/api/products")
    public Result<Page<Product>> page(@RequestParam(required = false) Long categoryId,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String origin,
                                      @RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size) {
        return Result.ok(productService.page(categoryId, keyword, origin, page, size, true));
    }

    @GetMapping("/api/products/{id}")
    public Result<Product> detail(@PathVariable Long id) {
        return Result.ok(productService.getById(id));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PostMapping("/api/products")
    public Result<Product> create(@RequestBody ProductDTO dto) {
        return Result.ok(productService.create(StpUtil.getLoginIdAsLong(), dto));
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/api/products/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ProductDTO dto) {
        productService.update(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @PutMapping("/api/products/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestBody Map<String, String> body) {
        productService.changeStatus(StpUtil.getLoginIdAsLong(), id, body.get("status"));
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("SELLER")
    @DeleteMapping("/api/products/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        productService.delete(StpUtil.getLoginIdAsLong(), id);
        return Result.ok(null);
    }
}
