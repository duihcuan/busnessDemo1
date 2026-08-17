package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class CartTest extends BaseTest {

    private String login() throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void addUpdateListRemove() throws Exception {
        String token = login();
        mockMvc.perform(post("/api/cart").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":1,\"quantity\":2}"))
                .andExpect(jsonPath("$.code").value(200));
        String list = mockMvc.perform(get("/api/cart").header("satoken", token))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("东坡泡菜·坛装老坛酸菜"))
                .andReturn().getResponse().getContentAsString();
        long cartId = com.jayway.jsonpath.JsonPath.parse(list).read("$.data[0].id", Long.class);
        mockMvc.perform(put("/api/cart/" + cartId).header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/api/cart/" + cartId).header("satoken", token))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/cart").header("satoken", token))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
