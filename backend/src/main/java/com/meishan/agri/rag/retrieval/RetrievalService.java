package com.meishan.agri.rag.retrieval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.rag.ai.EmbeddingClient;
import com.meishan.agri.rag.config.RagProperties;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RetrievalService {
    private final KbChunkMapper kbChunkMapper;
    private final KeywordRetriever keywordRetriever;
    private final EmbeddingClient embeddingClient;
    private final RagProperties props;

    public record Hit(KbChunk chunk, double score) {}

    public List<Hit> retrieve(String question) {
        List<KbChunk> chunks = kbChunkMapper.selectList(Wrappers.<KbChunk>lambdaQuery());
        if (chunks.isEmpty()) return List.of();
        List<String> texts = chunks.stream().map(KbChunk::getContent).toList();
        List<KeywordRetriever.Scored> kw = keywordRetriever.score(question, texts);

        float[] queryVec = null;
        if (embeddingClient.isEnabled()) {
            try { queryVec = embeddingClient.encode(question); }
            catch (Exception e) { log.warn("查询向量化失败，本次仅用关键词检索：{}", e.getMessage()); }
        }
        List<Hit> hits = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            double kwScore = kw.get(i).score();
            double vecScore = 0;
            if (queryVec != null && chunks.get(i).getVector() != null) {
                float[] cv = toFloats(chunks.get(i).getVector());
                vecScore = VectorRetriever.cosine(queryVec, cv);
            }
            double score = queryVec != null ? 0.6 * kwScore + 0.4 * vecScore : kwScore;
            if (score > 0) hits.add(new Hit(chunks.get(i), score));
        }
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        return hits.size() > props.getTopK() ? hits.subList(0, props.getTopK()) : hits;
    }

    private float[] toFloats(byte[] bytes) {
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        float[] out = new float[bytes.length / 4];
        for (int i = 0; i < out.length; i++) out[i] = buf.getFloat();
        return out;
    }
}
