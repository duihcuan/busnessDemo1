package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BaseTest {
    @Autowired
    protected MockMvc mockMvc;

    protected long idFrom(String json) {
        return com.jayway.jsonpath.JsonPath.parse(json).read("$.data.id", Long.class);
    }

    protected String tokenFrom(String json) {
        return com.jayway.jsonpath.JsonPath.parse(json).read("$.data.token", String.class);
    }

    @Test
    void pingReturnsOk() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(jsonPath("$.code").value(200));
    }
}
