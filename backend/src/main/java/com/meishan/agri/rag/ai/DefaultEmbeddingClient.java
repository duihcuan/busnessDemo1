package com.meishan.agri.rag.ai;

import com.meishan.agri.rag.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class DefaultEmbeddingClient implements EmbeddingClient {
    private final boolean enabled;

    public DefaultEmbeddingClient(RagProperties props) {
        Path p = Paths.get(props.getBgeModelPath());
        enabled = Files.exists(p);
        if (enabled) {
            log.info("检测到向量模型文件 {}，向量检索启用（需实现 ONNX 推理，见计划假设）", p);
        } else {
            log.warn("未找到 BGE 向量模型 {}，向量检索禁用，仅使用 BM25 关键词检索", props.getBgeModelPath());
        }
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public float[] encode(String text) {
        return null; // 本计划不实现 ONNX 推理；向量检索为后续扩展，余弦融合代码已就绪并被单测覆盖
    }
}
