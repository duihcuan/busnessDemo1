package com.meishan.agri.rag.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ChatResponse {
    private String answer;
    private String conversationId;
    private List<Map<String, String>> sources;
    private boolean offline;
    private Long assistantMessageId;
}
