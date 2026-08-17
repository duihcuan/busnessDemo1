package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthTest extends BaseTest {

    @Test
    void mockLoginReturnsTokenAndInfoWorks() throws Exception {
        String token = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/auth/info").header("satoken", token))
                .andExpect(jsonPath("$.data.role").value("CONSUMER"));
    }

    @Test
    void infoWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void adminLoginRejectsConsumer() throws Exception {
        mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"user1\",\"password\":\"123456\"}"))
                .andExpect(jsonPath("$.code").value(400));
    }
}
