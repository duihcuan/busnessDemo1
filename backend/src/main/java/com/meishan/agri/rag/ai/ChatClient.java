package com.meishan.agri.rag.ai;

import com.meishan.agri.rag.dto.ChatMessage;

import java.util.List;
import java.util.function.Consumer;

public interface ChatClient {
    String complete(List<ChatMessage> messages);
    void stream(List<ChatMessage> messages, Consumer<String> onDelta);
}
