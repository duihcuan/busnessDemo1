package com.meishan.agri.rag.dto;

import java.util.List;
import java.util.Map;

public record DeepSeekRequest(String model, List<Map<String, String>> messages, boolean stream) {}
