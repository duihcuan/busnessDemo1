package com.meishan.agri.rag.ingest;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.rag.ai.EmbeddingClient;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentIngestService {
    private final KbChunkMapper kbChunkMapper;
    private final EmbeddingClient embeddingClient;

    @Transactional
    public void ingest(KnowledgeDoc doc) {
        kbChunkMapper.delete(Wrappers.<KbChunk>lambdaQuery().eq(KbChunk::getDocId, doc.getId()));
        List<String> chunks = ChunkingUtil.split(doc.getContent(), 200, 20);
        for (int i = 0; i < chunks.size(); i++) {
            KbChunk c = new KbChunk();
            c.setDocId(doc.getId());
            c.setChunkIndex(i);
            c.setContent(chunks.get(i));
            if (embeddingClient.isEnabled()) {
                float[] v = null;
                try { v = embeddingClient.encode(chunks.get(i)); }
                catch (Exception e) { log.warn("第 {} 块向量化失败，跳过向量：{}", i, e.getMessage()); }
                c.setVector(v == null ? null : toBytes(v));
            }
            kbChunkMapper.insert(c);
        }
    }

    private byte[] toBytes(float[] v) {
        ByteBuffer buf = ByteBuffer.allocate(v.length * 4);
        for (float f : v) buf.putFloat(f);
        return buf.array();
    }
}
