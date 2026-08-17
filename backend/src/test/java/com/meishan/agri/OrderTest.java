package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class OrderTest extends BaseTest {

    private String login(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void fullOrderLifecycle() throws Exception {
        String buyer = login("13900000001");
        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":1,\"quantity\":2}]}"))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAY"))
                .andReturn().getResponse().getContentAsString();
        long orderId = idFrom(order);

        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"MOCK_WECHAT\"}"))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        String seller = login("13800000001");
        mockMvc.perform(put("/api/orders/" + orderId + "/ship").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logisticsCompany\":\"顺丰\",\"logisticsNo\":\"SF123456\"}"))
                .andExpect(jsonPath("$.data.status").value("SHIPPED"));

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));
    }

    @Test
    void illegalTransitionRejectedAndStockRestoredOnCancel() throws Exception {
        String buyer = login("13900000001");
        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":2,\"quantity\":1}]}"))
                .andReturn().getResponse().getContentAsString();
        long orderId = idFrom(order);

        mockMvc.perform(post("/api/orders/" + orderId + "/confirm").header("satoken", buyer))
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/orders/" + orderId + "/cancel").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void refundRestoresStock() throws Exception {
        String buyer = login("13900000001");
        String order = mockMvc.perform(post("/api/orders").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"addressId\":1,\"items\":[{\"productId\":5,\"quantity\":5}]}"))
                .andReturn().getResponse().getContentAsString();
        long orderId = idFrom(order);
        mockMvc.perform(post("/api/orders/" + orderId + "/pay").header("satoken", buyer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"MOCK_BALANCE\"}"))
                .andExpect(jsonPath("$.data.status").value("PAID"));
        mockMvc.perform(post("/api/orders/" + orderId + "/refund").header("satoken", buyer))
                .andExpect(jsonPath("$.data.status").value("REFUNDED"));

        mockMvc.perform(get("/api/products/5"))
                .andExpect(jsonPath("$.data.stock").value(80));
    }
}
