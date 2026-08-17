package com.meishan.agri;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class FileUploadTest extends BaseTest {

    private String login() throws Exception {
        return mockMvc.perform(post("/api/auth/mock-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"phone\":\"13800000001\",\"code\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void uploadImageReturnsUrl() throws Exception {
        String token = login();
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        mockMvc.perform(multipart("/api/files/upload").file(file).header("satoken", token))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").value(org.hamcrest.Matchers.startsWith("/uploads/")));
    }

    @Test
    void uploadExeRejected() throws Exception {
        String token = login();
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream", new byte[]{1});
        mockMvc.perform(multipart("/api/files/upload").file(file).header("satoken", token))
                .andExpect(jsonPath("$.code").value(400));
    }
}
