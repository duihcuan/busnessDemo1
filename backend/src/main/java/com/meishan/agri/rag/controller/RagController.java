package com.meishan.agri.rag.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.Result;
import com.meishan.agri.rag.dto.DocumentDTO;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.service.RagDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RagController {
    private final RagDocumentService documentService;

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PostMapping("/api/rag/documents")
    public Result<KnowledgeDoc> create(@RequestBody DocumentDTO dto) {
        return Result.ok(documentService.create(dto));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PutMapping("/api/rag/documents/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DocumentDTO dto) {
        documentService.update(id, dto);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/rag/documents/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @GetMapping("/api/rag/documents")
    public Result<Page<KnowledgeDoc>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.ok(documentService.page(page, size));
    }
}
