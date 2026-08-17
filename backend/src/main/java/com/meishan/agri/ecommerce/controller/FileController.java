package com.meishan.agri.ecommerce.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import com.meishan.agri.common.Result;
import com.meishan.agri.ecommerce.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @SaCheckLogin
    @PostMapping("/api/files/upload")
    public Result<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        return Result.ok(Map.of("url", fileService.upload(file)));
    }
}
