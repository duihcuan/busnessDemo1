package com.meishan.agri.rag.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meishan.agri.common.BizException;
import com.meishan.agri.rag.config.DeepSeekProperties;
import com.meishan.agri.rag.dto.ChatMessage;
import com.meishan.agri.rag.dto.DeepSeekRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DeepSeekChatClient implements ChatClient {
    private final DeepSeekProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String complete(List<ChatMessage> messages) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new BizException("未配置 DeepSeek API Key");
        }
        List<Map<String, String>> msgs = messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content())).toList();
        Map<?, ?> resp = RestClient.builder().build().post()
                .uri(props.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + props.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeepSeekRequest(props.getModel(), msgs, false))
                .retrieve().body(Map.class);
        Map<?, ?> choice = (Map<?, ?>) ((List<?>) resp.get("choices")).get(0);
        Map<?, ?> msg = (Map<?, ?>) choice.get("message");
        Object content = msg.get("content");
        return content == null ? "" : content.toString();
    }

    @Override
    public void stream(List<ChatMessage> messages, Consumer<String> onDelta) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new BizException("未配置 DeepSeek API Key");
        }
        try {
            URL url = new URL(props.getBaseUrl() + "/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + props.getApiKey());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            List<Map<String, String>> msgs = messages.stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content())).toList();
            String body = "{\"model\":\"" + props.getModel() + "\",\"stream\":true,\"messages\":"
                    + objectMapper.writeValueAsString(msgs) + "}";
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;
                String delta = parseDelta(data);
                if (delta != null && !delta.isEmpty()) onDelta.accept(delta);
            }
            reader.close();
        } catch (Exception e) {
            throw new BizException("DeepSeek 调用失败：" + e.getMessage());
        }
    }

    private String parseDelta(String json) {
        try {
            Map<?, ?> obj = objectMapper.readValue(json, Map.class);
            List<?> choices = (List<?>) obj.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<?, ?> delta = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("delta");
            Object c = delta.get("content");
            return c == null ? null : c.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
