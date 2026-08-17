package com.meishan.agri.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private String bgeModelPath = "backend/models/bge-small-zh-v1.5.onnx";
    private int topK = 3;
}
