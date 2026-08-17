package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class SellerAddressTest extends BaseTest {

    private String login(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void applyAndApproveMakesSeller() throws Exception {
        String userToken = login("13500000001");
        String applyResult = mockMvc.perform(post("/api/seller/apply")
                        .header("satoken", userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"shopName\":\"丹棱果园\",\"shopDesc\":\"自家果园直供\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        String sellerInfoId = applyResult.replaceAll(".*\"id\":(\\d+).*", "$1");

        String adminToken = mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/admin/sellers/" + sellerInfoId + "/approve").header("satoken", adminToken))
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/api/seller/info").header("satoken", userToken))
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }

    @Test
    void addressCrudWithDefault() throws Exception {
        String token = login("13900000001");
        mockMvc.perform(post("/api/addresses")
                        .header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"receiver\":\"李四\",\"phone\":\"13900000001\",\"province\":\"四川省\",\"city\":\"眉山市\",\"district\":\"丹棱县\",\"detail\":\"果园路 2 号\",\"isDefault\":1}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/addresses").header("satoken", token))
                .andExpect(jsonPath("$.data.length()").value(2));
    }
}
