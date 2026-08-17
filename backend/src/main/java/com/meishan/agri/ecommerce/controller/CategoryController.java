package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.entity.Category;
import com.meishan.agri.ecommerce.mapper.CategoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryMapper categoryMapper;

    @GetMapping("/api/categories")
    public Result<List<Category>> list() {
        return Result.ok(categoryMapper.selectList(
                Wrappers.<Category>lambdaQuery().orderByAsc(Category::getSort)));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PostMapping("/api/admin/categories")
    public Result<Category> create(@RequestBody Category category) {
        categoryMapper.insert(category);
        return Result.ok(category);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PutMapping("/api/admin/categories/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Category category) {
        category.setId(id);
        categoryMapper.updateById(category);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/admin/categories/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return Result.ok(null);
    }
}
