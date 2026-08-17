package com.meishan.agri;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class TraceTest extends BaseTest {

    @Test
    void queryByCodeReturnsFourStages() throws Exception {
        mockMvc.perform(get("/api/trace/MS-PC-001"))
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].stage").value("PLANT"));
    }

    @Test
    void unknownCodeReturnsEmpty() throws Exception {
        mockMvc.perform(get("/api/trace/NOT-EXIST"))
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
