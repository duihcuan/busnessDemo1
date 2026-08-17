package com.meishan.agri.rag.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.BizException;
import com.meishan.agri.rag.dto.DocumentDTO;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.ingest.DocumentIngestService;
import com.meishan.agri.rag.mapper.KnowledgeDocMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RagDocumentService {
    private final KnowledgeDocMapper docMapper;
    private final DocumentIngestService ingestService;

    @Transactional
    public KnowledgeDoc create(DocumentDTO dto) {
        KnowledgeDoc doc = new KnowledgeDoc();
        apply(doc, dto);
        doc.setStatus("ENABLED");
        docMapper.insert(doc);
        ingestService.ingest(doc);
        return doc;
    }

    @Transactional
    public void update(Long id, DocumentDTO dto) {
        KnowledgeDoc doc = docMapper.selectById(id);
        if (doc == null) throw new BizException("文档不存在");
        apply(doc, dto);
        docMapper.updateById(doc);
        ingestService.ingest(doc);
    }

    public void delete(Long id) {
        docMapper.deleteById(id);
    }

    public Page<KnowledgeDoc> page(int page, int size) {
        return docMapper.selectPage(Page.of(page, size),
                Wrappers.<KnowledgeDoc>lambdaQuery().orderByDesc(KnowledgeDoc::getCreateTime));
    }

    private void apply(KnowledgeDoc doc, DocumentDTO dto) {
        doc.setTitle(dto.getTitle());
        doc.setCategory(dto.getCategory());
        doc.setContent(dto.getContent());
        doc.setSource(dto.getSource());
    }
}
