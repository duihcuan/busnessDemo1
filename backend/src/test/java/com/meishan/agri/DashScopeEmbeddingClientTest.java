package com.meishan.agri;

import com.meishan.agri.rag.ai.DashScopeEmbeddingClient;
import com.meishan.agri.rag.config.DashScopeProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class DashScopeEmbeddingClientTest {

    @Test
    void disabledWithoutKey() {
        DashScopeProperties props = new DashScopeProperties();
        props.setApiKey("");
        DashScopeEmbeddingClient client = new DashScopeEmbeddingClient(props);
        assertFalse(client.isEnabled());
        assertNull(client.encode("测试"));
    }
}