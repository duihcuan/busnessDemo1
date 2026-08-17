package com.meishan.agri.rag.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.Result;
import com.meishan.agri.rag.dto.ChatRequest;
import com.meishan.agri.rag.dto.ChatResponse;
import com.meishan.agri.rag.dto.DocumentDTO;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.entity.RagMessage;
import com.meishan.agri.rag.service.RagChatService;
import com.meishan.agri.rag.service.RagDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@RestController
@RequiredArgsConstructor
public class RagController {
    private final RagDocumentService documentService;
    private final RagChatService chatService;
    private final ExecutorService executor = Executors.newCachedThreadPool();

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

    @SaCheckLogin
    @PostMapping("/api/rag/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest req) {
        return Result.ok(chatService.chat(StpUtil.getLoginIdAsLong(), req));
    }

    @SaCheckLogin
    @PostMapping(value = "/api/rag/chat", params = "stream=true", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest req) {
        SseEmitter emitter = new SseEmitter(0L);
        executor.submit(() -> {
            try {
                chatService.streamAnswer(StpUtil.getLoginIdAsLong(), req, delta -> {
                    try { emitter.send(SseEmitter.event().name("delta").data(delta)); }
                    catch (java.io.IOException ignored) {}
                });
                emitter.send(SseEmitter.event().name("done").data("END"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });
        return emitter;
    }

    @SaCheckLogin
    @PostMapping("/api/rag/messages/{id}/feedback")
    public Result<Void> feedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
        chatService.feedback(StpUtil.getLoginIdAsLong(), id, body.get("feedback"));
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @GetMapping("/api/rag/messages")
    public Result<Page<RagMessage>> messages(@RequestParam(defaultValue = "1") int page,
                                             @RequestParam(defaultValue = "10") int size) {
        return Result.ok(chatService.pageMessages(page, size));
    }
}
