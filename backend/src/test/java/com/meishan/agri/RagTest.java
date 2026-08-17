package com.meishan.agri;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class RagTest extends BaseTest {
    @Autowired
    private KbChunkMapper kbChunkMapper;

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
}
