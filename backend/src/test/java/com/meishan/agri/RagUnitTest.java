package com.meishan.agri;

import com.meishan.agri.rag.ingest.ChunkingUtil;
import com.meishan.agri.rag.retrieval.VectorRetriever;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagUnitTest {

    @Test
    void chunkingSplitsLongTextWithOverlap() {
        String text = "柑橘采摘后应尽快预冷。预冷温度控制在 4℃ 左右。".repeat(20);
        List<String> chunks = ChunkingUtil.split(text, 50, 10);
        assertTrue(chunks.size() >= 3);
        assertTrue(chunks.get(0).length() <= 50);
        assertTrue(chunks.get(1).startsWith(chunks.get(0).substring(chunks.get(0).length() - 10)));
    }

    @Test
    void cosineSimilarity() {
        float[] a = {1f, 0f};
        float[] b = {1f, 0f};
        float[] c = {0f, 1f};
        assertEquals(1.0f, VectorRetriever.cosine(a, b), 1e-5);
        assertTrue(VectorRetriever.cosine(a, c) < 0.01f);
    }
}
