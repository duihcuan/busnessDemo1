package com.meishan.agri;

import com.meishan.agri.rag.ai.ChatClient;
import com.meishan.agri.rag.dto.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

class ChatClientContractTest {
    @Test
    void interfaceContract() {
        ChatClient client = new ChatClient() {
            @Override
            public String complete(List<ChatMessage> messages) { return "stub"; }
            @Override
            public void stream(List<ChatMessage> messages, Consumer<String> onDelta) {}
        };
        org.junit.jupiter.api.Assertions.assertEquals("stub",
                client.complete(List.of(new ChatMessage("user", "你好"))));
    }
}
