package com.meishan.agri.rag.ingest;

import java.util.ArrayList;
import java.util.List;

public final class ChunkingUtil {
    private ChunkingUtil() {}

    public static List<String> split(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        String clean = text == null ? "" : text.replace("\r\n", "\n").trim();
        if (clean.isEmpty()) return chunks;
        int start = 0;
        while (start < clean.length()) {
            int end = Math.min(start + size, clean.length());
            chunks.add(clean.substring(start, end).trim());
            if (end == clean.length()) break;
            start = end - overlap;
        }
        return chunks;
    }
}
