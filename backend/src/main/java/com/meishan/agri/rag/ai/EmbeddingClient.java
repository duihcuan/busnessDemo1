package com.meishan.agri.rag.ai;

public interface EmbeddingClient {
    boolean isEnabled();
    float[] encode(String text);
}
