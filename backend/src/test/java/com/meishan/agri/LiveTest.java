package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class LiveTest extends BaseTest {

    private String login(String phone) throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"" + phone + "\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void publicRoomListOnlyShowsLive() throws Exception {
        String seller = login("13800000001");
        mockMvc.perform(post("/api/live/rooms").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"room\":{\"title\":\"丹棱桔橙采摘直播\",\"videoUrl\":\"https://example.com/v.mp4\"}}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/live/rooms"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void danmakuFlow() throws Exception {
        String user = login("13900000001");
        mockMvc.perform(post("/api/live/rooms/1/danmaku").header("satoken", user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"主播好！\"}"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/live/rooms/1/danmaku"))
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    void sellerSetsRoomProducts() throws Exception {
        String seller = login("13800000001");
        mockMvc.perform(put("/api/live/rooms/1/products").header("satoken", seller)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"productId\":2,\"livePrice\":88.00,\"sort\":1}]"))
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/api/live/rooms/1"))
                .andExpect(jsonPath("$.data.products.length()").value(1))
                .andExpect(jsonPath("$.data.products[0].productId").value(2));
    }
}
