package com.meishan.agri.rag.dto;

import lombok.Data;

@Data
public class ChatRequest {
    private String question;
    private String conversationId;
}
