package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class ProductTest extends BaseTest {

    @Test
    void publicListFiltersOffSale() throws Exception {
        mockMvc.perform(get("/api/products").param("page", "1").param("size", "10"))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));
    }

    @Test
    void sellerCanCreateAndOffSale() throws Exception {
        String token = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String body = mockMvc.perform(post("/api/products")
                        .header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryId\":2,\"name\":\"丹棱桔橙·家庭装\",\"specText\":\"5kg/箱\",\"price\":45.00,\"stock\":60,\"origin\":\"丹棱县\",\"traceCode\":\"MS-GJ-003\",\"description\":\"现摘直发\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long id = idFrom(body);

        mockMvc.perform(put("/api/products/" + id + "/status")
                        .header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OFF_SALE\"}"))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(jsonPath("$.data.status").value("OFF_SALE"));

        mockMvc.perform(get("/api/products").param("categoryId", "2"))
                .andExpect(jsonPath("$.data.total").value(2));
    }

    @Test
    void nonSellerCannotCreateProduct() throws Exception {
        String token = mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13900000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/products").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"x\",\"price\":1.00,\"stock\":1}"))
                .andExpect(jsonPath("$.code").value(403));
    }
}
