package com.meishan.agri.system.controller;

import com.meishan.agri.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {
    @GetMapping("/api/ping")
    public Result<Map<String, String>> ping() {
        return Result.ok(Map.of("msg", "pong"));
    }
}
