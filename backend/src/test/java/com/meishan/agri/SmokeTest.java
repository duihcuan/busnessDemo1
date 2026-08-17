package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class SmokeTest extends BaseTest {

    @Test
    void sellerStatsAndAdminStats() throws Exception {
        String seller = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/seller/stats").header("satoken", seller))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.productCount").value(6));

        String admin = mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(get("/api/admin/stats").header("satoken", admin))
                .andExpect(jsonPath("$.data.userCount").value(3));
    }

    @Test
    void fullDemoFlow() throws Exception {
        String buyer = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":3,\"quantity\":1}]}"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"))
                .andReturn().getResponse().getContentAsString();
        long orderId = idFrom(order);

        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"MOCK_WECHAT\"}"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        String seller = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
        mockMvc.perform(put("/api/orders/" + orderId + "/ship").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsCompany\":\"顺丰\",\"logisticsNo\":\"SF999\"}"))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        mockMvc.perform(get("/api/trace/MS-GJ-001"))
                .andExpect(jsonPath("$.data.length()").value(4));

        mockMvc.perform(get("/api/live/rooms"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }
}
