package com.meishan.agri.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "dashscope")
public class DashScopeProperties {
    private String apiKey = "";
    private String baseUrl = "https://dashscope.aliyuncs.com";
    private String model = "text-embedding-v3";
}