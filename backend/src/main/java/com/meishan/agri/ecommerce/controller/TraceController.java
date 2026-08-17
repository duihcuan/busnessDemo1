package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.entity.TraceRecord;
import com.meishan.agri.ecommerce.service.TraceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TraceController {
    private final TraceService traceService;

    @GetMapping("/api/trace/{code}")
    public Result<List<TraceRecord>> findByCode(@PathVariable String code) {
        return Result.ok(traceService.findByCode(code));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @GetMapping("/api/trace")
    public Result<List<TraceRecord>> list() {
        return Result.ok(traceService.listAll());
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PostMapping("/api/trace")
    public Result<TraceRecord> create(@RequestBody TraceRecord record) {
        return Result.ok(traceService.create(record));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PutMapping("/api/trace/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody TraceRecord record) {
        record.setId(id);
        traceService.update(record);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/trace/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        traceService.delete(id);
        return Result.ok(null);
    }
}
