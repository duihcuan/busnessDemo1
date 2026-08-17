package com.meishan.agri.rag.runner;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.ingest.DocumentIngestService;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import com.meishan.agri.rag.mapper.KnowledgeDocMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSeedRunner implements ApplicationRunner {
    private final KnowledgeDocMapper docMapper;
    private final KbChunkMapper chunkMapper;
    private final DocumentIngestService ingestService;

    @Override
    public void run(ApplicationArguments args) {
        List<KnowledgeDoc> docs = docMapper.selectList(Wrappers.<KnowledgeDoc>lambdaQuery()
                .eq(KnowledgeDoc::getStatus, "ENABLED"));
        int ingested = 0;
        for (KnowledgeDoc doc : docs) {
            Long count = chunkMapper.selectCount(Wrappers.<KbChunk>lambdaQuery()
                    .eq(KbChunk::getDocId, doc.getId()));
            if (count == 0) {
                ingestService.ingest(doc);
                ingested++;
            }
        }
        log.info("知识库启动入库完成：{} 篇新文档已切分", ingested);
    }
}
