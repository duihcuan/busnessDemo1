package com.meishan.agri;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class RagTest extends BaseTest {
    @Autowired
    private KbChunkMapper kbChunkMapper;

    @MockBean
    private com.meishan.agri.rag.ai.ChatClient chatClient;

    private String adminToken() throws Exception {
        return mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void createDocumentSplitsIntoChunks() throws Exception {
        String token = adminToken();
        String body = mockMvc.perform(post("/api/rag/documents").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"柑橘储存指南\",\"category\":\"TECH\",\"source\":\"平台\",\"content\":\"" + "柑橘采收后 24 小时内预冷至 4℃，可显著延长保鲜期。".repeat(15) + "\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long docId = idFrom(body);
        Long chunkCount = kbChunkMapper.selectCount(
                Wrappers.<KbChunk>lambdaQuery().eq(KbChunk::getDocId, docId));
        Assertions.assertTrue(chunkCount >= 3, "应切分出多个块");

        mockMvc.perform(get("/api/rag/documents").header("satoken", token))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void chatReturnsAnswerWithSources() throws Exception {
        org.mockito.Mockito.when(chatClient.complete(org.mockito.ArgumentMatchers.anyList()))
                .thenReturn("柑橘储存建议：采收后 24 小时内预冷至 4℃。");
        String token = adminToken();
        mockMvc.perform(post("/api/rag/documents").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"柑橘储存指南\",\"category\":\"TECH\",\"source\":\"平台\",\"content\":\"柑橘采收后 24 小时内预冷至 4℃，可显著延长保鲜期。通风保湿避免失水。\"}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/rag/chat").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"柑橘怎么储存？\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.answer").value("柑橘储存建议：采收后 24 小时内预冷至 4℃。"))
                .andExpect(jsonPath("$.data.sources.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void chatFallsBackOfflineWhenAiFails() throws Exception {
        org.mockito.Mockito.when(chatClient.complete(org.mockito.ArgumentMatchers.anyList()))
                .thenThrow(new RuntimeException("network down"));
        String token = adminToken();
        mockMvc.perform(post("/api/rag/documents").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"政策指南\",\"category\":\"POLICY\",\"source\":\"平台\",\"content\":\"眉山市助农电商补贴申报流程：农户在平台提交资质后由管理员审核。\"}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/api/rag/chat").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"补贴怎么申报？\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.offline").value(true))
                .andExpect(jsonPath("$.data.sources.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void feedbackUpdatesMessage() throws Exception {
        String token = adminToken();
        String body = mockMvc.perform(post("/api/rag/chat").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\":\"你好\"}"))
                .andReturn().getResponse().getContentAsString();
        long msgId = com.jayway.jsonpath.JsonPath.parse(body).read("$.data.assistantMessageId", Long.class);
        mockMvc.perform(post("/api/rag/messages/" + msgId + "/feedback").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"feedback\":\"UP\"}"))
                .andExpect(jsonPath("$.code").value(200));
    }
}
