package com.meishan.agri.rag.ai;

import com.meishan.agri.rag.config.DashScopeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Primary
@Component
@RequiredArgsConstructor
public class DashScopeEmbeddingClient implements EmbeddingClient {
    private final DashScopeProperties props;

    @Override
    public boolean isEnabled() {
        return props.getApiKey() != null && !props.getApiKey().isBlank();
    }

    @Override
    public float[] encode(String text) {
        if (!isEnabled()) return null;
        try {
            Map<?, ?> resp = RestClient.builder().build().post()
                    .uri(props.getBaseUrl() + "/compatible-mode/v1/embeddings")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("model", props.getModel(), "input", text))
                    .retrieve().body(Map.class);
            List<?> data = (List<?>) resp.get("data");
            Map<?, ?> first = (Map<?, ?>) data.get(0);
            List<?> emb = (List<?>) first.get("embedding");
            float[] out = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) out[i] = ((Number) emb.get(i)).floatValue();
            return out;
        } catch (Exception e) {
            log.error("DashScope 向量化失败：{}", e.getMessage());
            throw new RuntimeException("向量化失败：" + e.getMessage(), e);
        }
    }
}