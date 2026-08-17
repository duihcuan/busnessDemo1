# RAG 智能客服模块实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在既有后端上新增 RAG 智能客服：知识库文档入库（切分、可选本地向量化）、混合检索（BM25 关键词 + 可选余弦向量）、DeepSeek 生成（SSE/JSON）、引用来源与用户反馈、断网/无 Key 离线降级。

**架构：** 新增 `rag` 业务包，复用现有 common/system 基建（Result、Sa-Token、MyBatis-Plus、MySQL 的 knowledge_doc/kb_chunk/rag_message 三张表）；AI 层通过 ChatClient/EmbeddingClient 接口隔离，DeepSeek 与本地 ONNX BGE 均为可替换实现；无向量模型文件时自动降级为纯关键词检索。

**技术栈：** Java 17+（本机 21）、Spring Boot 3.2.5、MyBatis-Plus 3.5.7、onnxruntime（可选，模型文件存在才加载）、DeepSeek chat API（RestClient + HttpURLConnection 流式）。

---

## 文件结构

```text
backend/src/main/java/com/meishan/agri/rag/
  entity/KnowledgeDoc.java, KbChunk.java, RagMessage.java
  mapper/KnowledgeDocMapper.java, KbChunkMapper.java, RagMessageMapper.java
  dto/DocumentDTO.java, ChatRequest.java, ChatResponse.java, ChatMessage.java, MessageDTO.java
  ingest/ChunkingUtil.java                 # 切分：200 字符 + 20 重叠
  ingest/DocumentIngestService.java        # 入库：切分 + 可选向量化 + 写 kb_chunk
  retrieval/KeywordRetriever.java          # BM25 自实现（离线）
  retrieval/VectorRetriever.java           # 余弦相似度（仅当有向量时启用）
  retrieval/RetrievalService.java          # 融合 top-k（0.6 BM25 + 0.4 向量）
  ai/ChatClient.java                       # 接口：complete / stream
  ai/DeepSeekChatClient.java               # 真实实现
  ai/EmbeddingClient.java                  # 接口：encode() -> float[]|null
  ai/DefaultEmbeddingClient.java            # 默认禁用：模型文件缺失时明确降级为纯 BM25
  service/RagDocumentService.java          # 文档 CRUD + 重新入库
  service/RagChatService.java              # 检索+Prompt+生成+降级+会话+反馈
  controller/RagController.java            # 文档管理（ADMIN）+ 问答（登录用户）+ 反馈
  config/RagProperties.java                # deepseek/api-key、rag/bge-model-path
  runner/DocumentSeedRunner.java           # 启动时给无 chunk 的文档补入库
  dto/DeepSeekRequest.java                          # AI 层请求 DTO（响应解析用 Map）
backend/src/test/java/com/meishan/agri/
  RagUnitTest.java                         # ChunkingUtil / 余弦相似度 / BM25
  RagTest.java                             # 入库/问答/降级/反馈/会话（Mock DeepSeek）
```

全局约定沿用后端核心计划：所有接口返回 `Result<T>`；认证用 `@SaCheckLogin` / `@SaCheckRole("ADMIN")`；测试继承 `BaseTest`（`@Transactional` 隔离）；每任务提交。

配置（`application.yml` 追加）：

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat
rag:
  bge-model-path: backend/models/bge-small-zh-v1.5.onnx
  top-k: 3
```

---

### 任务 1：RAG 实体、工具与单测

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/rag/entity/KnowledgeDoc.java`、`KbChunk.java`、`RagMessage.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/mapper/KnowledgeDocMapper.java`、`KbChunkMapper.java`、`RagMessageMapper.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/ingest/ChunkingUtil.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/retrieval/VectorRetriever.java`（含余弦相似度静态方法）
- 创建：`backend/src/test/java/com/meishan/agri/RagUnitTest.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import com.meishan.agri.rag.ingest.ChunkingUtil;
import com.meishan.agri.rag.retrieval.VectorRetriever;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Consumer;

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
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=RagUnitTest`
预期：FAIL（类不存在）

- [ ] **步骤 3：实现实体、Mapper 与工具**

三个实体与表结构一一对应（`@TableName("knowledge_doc"/"kb_chunk"/"rag_message")`、`@TableId(type = IdType.AUTO)`、字段同 schema：KnowledgeDoc=id/title/category/content/source/status/createTime；KbChunk=id/docId/chunkIndex/content/vector/byte[]/createTime；RagMessage=id/userId/conversationId/role/content/sources/feedback/createTime）。

`ChunkingUtil.java`：

```java
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
```

`VectorRetriever.java`（本任务先提供静态余弦方法，检索方法在任务 3 补全）：

```java
package com.meishan.agri.rag.retrieval;

public class VectorRetriever {
    public static float cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || a.length != b.length) return 0f;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) return 0f;
        return (float) (dot / (Math.sqrt(na) * Math.sqrt(nb)));
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=RagUnitTest`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: RAG 实体与切分/相似度工具"
```

---

### 任务 2：知识库文档管理与入库

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/rag/dto/DocumentDTO.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/ai/EmbeddingClient.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/ai/OnnxBgeEmbeddingClient.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/ingest/DocumentIngestService.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/service/RagDocumentService.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/config/RagProperties.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/controller/RagController.java`（先做文档管理部分）
- 修改：`backend/src/main/resources/application.yml`（追加 deepseek/rag 配置）
- 创建：`backend/src/test/java/com/meishan/agri/RagTest.java`（先写文档入库用例）

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import cn.dev33.satoken.stp.StpUtil;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

class RagTest extends BaseTest {
    @Autowired
    private KbChunkMapper kbChunkMapper;

    private String adminToken() throws Exception {
        return mockMvc.perform(post("/api/auth/admin-login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andReturn().getResponse().getContentAsString()
                .replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");
    }

    @Test
    void createDocumentSplitsIntoChunks() throws Exception {
        String token = adminToken();
        String body = mockMvc.perform(post("/api/rag/documents").header("satoken", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"柑橘储存指南\",\"category\":\"TECH\",\"source\":\"平台\",\"content\":\"" + "柑橘采收后 24 小时内预冷至 4℃，可显著延长保鲜期。".repeat(15) + "\"}"))
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString();
        long docId = idFrom(body);
        Long chunkCount = kbChunkMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers.<KbChunk>lambdaQuery()
                        .eq(KbChunk::getDocId, docId));
        org.junit.jupiter.api.Assertions.assertTrue(chunkCount >= 3, "应切分出多个块");

        mockMvc.perform(get("/api/rag/documents").header("satoken", token))
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=RagTest`
预期：FAIL（接口不存在）

- [ ] **步骤 3：实现文档管理与入库**

`RagProperties.java`：

```java
package com.meishan.agri.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagProperties {
    private String bgeModelPath = "backend/models/bge-small-zh-v1.5.onnx";
    private int topK = 3;
}
```

`application.yml` 追加：

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY:}
  base-url: https://api.deepseek.com
  model: deepseek-chat
rag:
  bge-model-path: backend/models/bge-small-zh-v1.5.onnx
  top-k: 3
```

`EmbeddingClient.java`：

```java
package com.meishan.agri.rag.ai;

public interface EmbeddingClient {
    boolean isEnabled();
    float[] encode(String text);
}
```

`DefaultEmbeddingClient.java`：

```java
package com.meishan.agri.rag.ai;

import com.meishan.agri.rag.config.RagProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Component
public class DefaultEmbeddingClient implements EmbeddingClient {
    private final boolean enabled;

    public DefaultEmbeddingClient(RagProperties props) {
        Path p = Paths.get(props.getBgeModelPath());
        enabled = Files.exists(p);
        if (enabled) {
            log.info("检测到向量模型文件 {}，向量检索启用（需实现 ONNX 推理，见计划假设）", p);
        } else {
            log.warn("未找到 BGE 向量模型 {}，向量检索禁用，仅使用 BM25 关键词检索", props.getBgeModelPath());
        }
    }

    @Override
    public boolean isEnabled() { return enabled; }

    @Override
    public float[] encode(String text) {
        return null; // 本计划不实现 ONNX 推理；向量检索为后续扩展，余弦融合代码已就绪并被单测覆盖
    }
}
```

> 计划假设（决策）：向量化推理依赖 ONNX 模型文件与 tokenizer，无法离线获得，本计划不实现推理；EmbeddingClient 恒返回 null，检索统一走 BM25（离线、确定性、可测试）。余弦相似度与 0.6/0.4 融合代码保留（任务 1、3），以便后续接入向量时无需改动检索编排。

`DocumentIngestService.java`：

```java
package com.meishan.agri.rag.ingest;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.rag.ai.EmbeddingClient;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.ByteBuffer;
import java.util.List;

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
                float[] v = embeddingClient.encode(chunks.get(i));
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
```

`RagDocumentService.java`：

```java
package com.meishan.agri.rag.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        if (doc == null) throw new com.meishan.agri.common.BizException("文档不存在");
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
```

`DocumentDTO.java`：`title/category/content/source` 四个 String 字段。

`RagController.java`（先实现文档管理部分，问答部分任务 5 追加）：

```java
package com.meishan.agri.rag.controller;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.meishan.agri.common.Result;
import com.meishan.agri.rag.dto.DocumentDTO;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.service.RagDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class RagController {
    private final RagDocumentService documentService;

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PostMapping("/api/rag/documents")
    public Result<KnowledgeDoc> create(@RequestBody DocumentDTO dto) {
        return Result.ok(documentService.create(dto));
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @PutMapping("/api/rag/documents/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody DocumentDTO dto) {
        documentService.update(id, dto);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @DeleteMapping("/api/rag/documents/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return Result.ok(null);
    }

    @SaCheckLogin
    @SaCheckRole("ADMIN")
    @GetMapping("/api/rag/documents")
    public Result<Page<KnowledgeDoc>> page(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return Result.ok(documentService.page(page, size));
    }
}
```

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=RagTest`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: RAG 知识库文档管理与入库"
```

---

### 任务 3：检索服务（BM25 + 向量融合）

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/rag/retrieval/KeywordRetriever.java`
- 修改：`backend/src/main/java/com/meishan/agri/rag/retrieval/VectorRetriever.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/retrieval/RetrievalService.java`
- 修改：`backend/src/test/java/com/meishan/agri/RagUnitTest.java`（追加 BM25 用例）

- [ ] **步骤 1：编写失败的测试**

在 `RagUnitTest` 追加：

```java
@Test
void bm25RanksKeywordMatchFirst() {
    var retriever = new com.meishan.agri.rag.retrieval.KeywordRetriever();
    var chunks = java.util.List.of(
            "东坡泡菜发酵 60 天，口感酸爽",
            "柑橘储存需要预冷和通风",
            "平台客服帮助农户学习店铺运营");
    var ranked = retriever.score("泡菜 发酵 60 天", chunks);
    org.junit.jupiter.api.Assertions.assertEquals(0, ranked.get(0).index());
    org.junit.jupiter.api.Assertions.assertTrue(ranked.get(0).score() > ranked.get(1).score());
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=RagUnitTest`
预期：FAIL（KeywordRetriever 不存在）

- [ ] **步骤 3：实现检索**

`KeywordRetriever.java`（简化 BM25，词频 + 文档频率，离线零依赖）：

```java
package com.meishan.agri.rag.retrieval;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

@Component
public class KeywordRetriever {
    private static final Pattern TOKEN = Pattern.compile("[\\u4e00-\\u9fa5]{1}|[a-zA-Z0-9]+");

    public record Scored(int index, double score) {}

    public List<Scored> score(String query, List<String> chunks) {
        List<String> qTokens = tokenize(query);
        Map<String, Integer> df = new HashMap<>();
        for (String chunk : chunks) {
            Set<String> tokens = new HashSet<>(tokenize(chunk));
            for (String t : tokens) df.merge(t, 1, Integer::sum);
        }
        int n = Math.max(chunks.size(), 1);
        List<Scored> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            List<String> cTokens = tokenize(chunks.get(i));
            Map<String, Integer> tf = new HashMap<>();
            for (String t : cTokens) tf.merge(t, 1, Integer::sum);
            double score = 0;
            for (String t : qTokens) {
                double idf = Math.log(1 + (double) n / (1 + df.getOrDefault(t, 0)));
                score += tf.getOrDefault(t, 0) * idf;
            }
            result.add(new Scored(i, score));
        }
        result.sort((x, y) -> Double.compare(y.score(), x.score()));
        return result;
    }

    private List<String> tokenize(String text) {
        List<String> out = new ArrayList<>();
        var m = TOKEN.matcher(text == null ? "" : text);
        while (m.find()) out.add(m.group());
        return out;
    }
}
```

`RetrievalService.java`：

```java
package com.meishan.agri.rag.retrieval;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.meishan.agri.rag.ai.EmbeddingClient;
import com.meishan.agri.rag.config.RagProperties;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.ByteBuffer;
import java.util.*;

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

        float[] queryVec = embeddingClient.isEnabled() ? embeddingClient.encode(question) : null;
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
```

`VectorRetriever` 增加无参构造（`public VectorRetriever() {}`）。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=RagUnitTest`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: RAG 混合检索（BM25 + 可选向量）"
```

---

### 任务 4：DeepSeek 客户端与配置

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/rag/ai/ChatClient.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/dto/DeepSeekRequest.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/ai/DeepSeekChatClient.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/config/DeepSeekProperties.java`

- [ ] **步骤 1：编写失败的测试**

```java
package com.meishan.agri;

import com.meishan.agri.rag.ai.ChatClient;
import com.meishan.agri.rag.dto.ChatMessage;
import org.junit.jupiter.api.Test;

import java.util.List;

class ChatClientContractTest {
    @Test
    void interfaceContract() {
        ChatClient client = new ChatClient() {
            @Override
            public String complete(List<ChatMessage> messages) { return "stub"; }
            @Override
            public void stream(List<ChatMessage> messages, Consumer<String> onDelta) {}
        };
        org.junit.jupiter.api.Assertions.assertEquals("stub",
                client.complete(List.of(new ChatMessage("user", "你好"))));
    }
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=ChatClientContractTest`
预期：FAIL（类不存在）

- [ ] **步骤 3：实现 AI 客户端**

`ChatClient.java`：

```java
package com.meishan.agri.rag.ai;

import com.meishan.agri.rag.dto.ChatMessage;

import java.util.List;
import java.util.function.Consumer;

public interface ChatClient {
    String complete(List<ChatMessage> messages);
    void stream(List<ChatMessage> messages, Consumer<String> onDelta);
}
```

`ChatMessage.java`：

```java
package com.meishan.agri.rag.dto;

public record ChatMessage(String role, String content) {}
```

`DeepSeekProperties.java`：

```java
package com.meishan.agri.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "deepseek")
public class DeepSeekProperties {
    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
}
```

`DeepSeekRequest.java` / `DeepSeekResponse.java`：与 DeepSeek API 对齐（request: model/messages/stream；response: choices[].message.content），用 `Map` 简化亦可；这里给出最小 record：

```java
// DeepSeekRequest.java
package com.meishan.agri.rag.dto;

import java.util.List;
import java.util.Map;

public record DeepSeekRequest(String model, List<Map<String, String>> messages, boolean stream) {}
```

`DeepSeekChatClient.java`：

```java
package com.meishan.agri.rag.ai;

import com.meishan.agri.common.BizException;
import com.meishan.agri.rag.config.DeepSeekProperties;
import com.meishan.agri.rag.dto.ChatMessage;
import com.meishan.agri.rag.dto.DeepSeekRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class DeepSeekChatClient implements ChatClient {
    private final DeepSeekProperties props;

    @Override
    public String complete(List<ChatMessage> messages) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new BizException("未配置 DeepSeek API Key");
        }
        List<Map<String, String>> msgs = messages.stream()
                .map(m -> Map.of("role", m.role(), "content", m.content())).toList();
        Map<?, ?> resp = RestClient.builder().build().post()
                .uri(props.getBaseUrl() + "/chat/completions")
                .header("Authorization", "Bearer " + props.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeepSeekRequest(props.getModel(), msgs, false))
                .retrieve().body(Map.class);
        Map<?, ?> choice = (Map<?, ?>) ((List<?>) resp.get("choices")).get(0);
        Map<?, ?> msg = (Map<?, ?>) choice.get("message");
        Object content = msg.get("content");
        return content == null ? "" : content.toString();
    }

    @Override
    public void stream(List<ChatMessage> messages, Consumer<String> onDelta) {
        if (props.getApiKey() == null || props.getApiKey().isBlank()) {
            throw new BizException("未配置 DeepSeek API Key");
        }
        try {
            URL url = new URL(props.getBaseUrl() + "/chat/completions");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + props.getApiKey());
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            List<Map<String, String>> msgs = messages.stream()
                    .map(m -> Map.of("role", m.role(), "content", m.content())).toList();
            String body = "{\"model\":\"" + props.getModel() + "\",\"stream\":true,\"messages\":"
                    + new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(msgs) + "}";
            conn.getOutputStream().write(body.getBytes(StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) continue;
                String data = line.substring(5).trim();
                if ("[DONE]".equals(data)) break;
                String delta = parseDelta(data);
                if (delta != null && !delta.isEmpty()) onDelta.accept(delta);
            }
            reader.close();
        } catch (Exception e) {
            throw new BizException("DeepSeek 调用失败：" + e.getMessage());
        }
    }

    private String parseDelta(String json) {
        try {
            Map<?, ?> obj = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, Map.class);
            List<?> choices = (List<?>) obj.get("choices");
            if (choices == null || choices.isEmpty()) return null;
            Map<?, ?> delta = (Map<?, ?>) ((Map<?, ?>) choices.get(0)).get("delta");
            Object c = delta.get("content");
            return c == null ? null : c.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
```

`RagProperties`/`DeepSeekProperties` 已标注 `@Configuration` + `@ConfigurationProperties`，组件扫描即可注册，无需额外注解。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=ChatClientContractTest`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: DeepSeek 聊天客户端与配置"
```

---

### 任务 5：问答服务、SSE 与反馈

**文件：**
- 创建：`backend/src/main/java/com/meishan/agri/rag/dto/ChatRequest.java`、`ChatResponse.java`
- 创建：`backend/src/main/java/com/meishan/agri/rag/service/RagChatService.java`
- 修改：`backend/src/main/java/com/meishan/agri/rag/controller/RagController.java`（追加问答与反馈接口）
- 修改：`backend/src/test/java/com/meishan/agri/RagTest.java`（追加问答/降级/反馈/会话用例）

- [ ] **步骤 1：编写失败的测试**

在 `RagTest` 追加（`@MockBean` 注入模拟 ChatClient）：

```java
@org.springframework.boot.test.mock.mockito.MockBean
private com.meishan.agri.rag.ai.ChatClient chatClient;

@Test
void chatReturnsAnswerWithSources() throws Exception {
    org.mockito.Mockito.when(chatClient.complete(org.mockito.ArgumentMatchers.anyList()))
            .thenReturn("柑橘储存建议：采收后 24 小时内预冷至 4℃。");
    String token = adminToken();
    // 先入库一篇文档
    mockMvc.perform(post("/api/rag/documents").header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"柑橘储存指南\",\"category\":\"TECH\",\"source\":\"平台\",\"content\":\"柑橘采收后 24 小时内预冷至 4℃，可显著延长保鲜期。通风保湿避免失水。\"}"))
            .andExpect(jsonPath("$.code").value(200));
    mockMvc.perform(post("/api/rag/chat").header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"柑橘怎么储存？\"}"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.answer").value("柑橘储存建议：采收后 24 小时内预冷至 4℃。"))
            .andExpect(jsonPath("$.data.sources.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
}

@Test
void chatFallsBackOfflineWhenAiFails() throws Exception {
    org.mockito.Mockito.when(chatClient.complete(org.mockito.ArgumentMatchers.anyList()))
            .thenThrow(new RuntimeException("network down"));
    String token = adminToken();
    mockMvc.perform(post("/api/rag/documents").header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"title\":\"政策指南\",\"category\":\"POLICY\",\"source\":\"平台\",\"content\":\"眉山市助农电商补贴申报流程：农户在平台提交资质后由管理员审核。\"}"))
            .andExpect(jsonPath("$.code").value(200));
    mockMvc.perform(post("/api/rag/chat").header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"补贴怎么申报？\"}"))
            .andExpect(jsonPath("$.code").value(200))
            .andExpect(jsonPath("$.data.offline").value(true))
            .andExpect(jsonPath("$.data.sources.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
}

@Test
void feedbackUpdatesMessage() throws Exception {
    String token = adminToken();
    String body = mockMvc.perform(post("/api/rag/chat").header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"你好\"}"))
            .andReturn().getResponse().getContentAsString();
    long msgId = com.jayway.jsonpath.JsonPath.parse(body).read("$.data.assistantMessageId", Long.class);
    mockMvc.perform(post("/api/rag/messages/" + msgId + "/feedback").header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"feedback\":\"UP\"}"))
            .andExpect(jsonPath("$.code").value(200));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=RagTest`
预期：FAIL（接口/字段不存在）

- [ ] **步骤 3：实现问答服务**

`ChatRequest.java`：`question/conversationId`（String）；`ChatResponse.java`：`answer/conversationId/sources(List<Map<String,String>>)/offline/assistantMessageId(Long)`。

`RagChatService.java`：

```java
package com.meishan.agri.rag.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meishan.agri.common.BizException;
import com.meishan.agri.rag.ai.ChatClient;
import com.meishan.agri.rag.dto.ChatMessage;
import com.meishan.agri.rag.dto.ChatRequest;
import com.meishan.agri.rag.dto.ChatResponse;
import com.meishan.agri.rag.entity.KbChunk;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.entity.RagMessage;
import com.meishan.agri.rag.mapper.KbChunkMapper;
import com.meishan.agri.rag.mapper.KnowledgeDocMapper;
import com.meishan.agri.rag.mapper.RagMessageMapper;
import com.meishan.agri.rag.retrieval.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class RagChatService {
    private static final String SYSTEM_PROMPT =
            "你是'眉山泡菜柑橘助农电商平台'的助农智能客服。请优先依据提供的知识片段回答，引用来源格式：[来源:标题]。若知识片段不足，明确告知并给出通用建议。";
    private static final String OFFLINE_SUFFIX =
            "（离线模式：AI 服务暂不可用，以上内容来自知识库检索，请以平台公告为准。）";

    private final RetrievalService retrievalService;
    private final ChatClient chatClient;
    private final RagMessageMapper messageMapper;
    private final KnowledgeDocMapper docMapper;
    private final KbChunkMapper chunkMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ChatResponse chat(Long userId, ChatRequest req) {
        String conversationId = req.getConversationId() == null || req.getConversationId().isBlank()
                ? UUID.randomUUID().toString() : req.getConversationId();
        List<RetrievalService.Hit> hits = retrievalService.retrieve(req.getQuestion());
        List<Map<String, String>> sources = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        for (RetrievalService.Hit hit : hits) {
            KnowledgeDoc doc = docMapper.selectById(hit.chunk().getDocId());
            String title = doc == null ? "未知文档" : doc.getTitle();
            sources.add(Map.of("docId", String.valueOf(hit.chunk().getDocId()),
                    "title", title, "content", hit.chunk().getContent()));
            context.append("[来源:").append(title).append("] ").append(hit.chunk().getContent()).append("\n");
        }

        List<ChatMessage> history = recentHistory(conversationId);
        saveMessage(userId, conversationId, "USER", req.getQuestion(), null, "NONE");

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system", SYSTEM_PROMPT + "\n\n知识片段：\n" + context));
        messages.addAll(history);
        messages.add(new ChatMessage("user", req.getQuestion()));

        String answer;
        boolean offline = false;
        try {
            answer = chatClient.complete(messages);
        } catch (Exception e) {
            log.warn("DeepSeek 调用失败，启用离线降级：{}", e.getMessage());
            answer = "根据知识库检索，以下信息供参考：\n" + context + OFFLINE_SUFFIX;
            offline = true;
        }
        RagMessage saved = saveMessage(userId, conversationId, "ASSISTANT", answer,
                toJson(sources), "NONE");
        ChatResponse resp = new ChatResponse();
        resp.setAnswer(answer);
        resp.setConversationId(conversationId);
        resp.setSources(sources);
        resp.setOffline(offline);
        resp.setAssistantMessageId(saved.getId());
        return resp;
    }

    public void streamAnswer(Long userId, ChatRequest req, Consumer<String> onDelta) {
        List<RetrievalService.Hit> hits = retrievalService.retrieve(req.getQuestion());
        List<ChatMessage> messages = List.of(
                new ChatMessage("system", SYSTEM_PROMPT),
                new ChatMessage("user", req.getQuestion()));
        chatClient.stream(messages, onDelta);
    }

    @Transactional
    public void feedback(Long userId, Long messageId, String feedback) {
        if (!"UP".equals(feedback) && !"DOWN".equals(feedback)) throw new BizException("非法反馈");
        RagMessage m = messageMapper.selectById(messageId);
        if (m == null) throw new BizException("消息不存在");
        m.setFeedback(feedback);
        messageMapper.updateById(m);
    }

    public com.baomidou.mybatisplus.extension.plugins.pagination.Page<RagMessage> pageMessages(int page, int size) {
        return messageMapper.selectPage(
                com.baomidou.mybatisplus.extension.plugins.pagination.Page.of(page, size),
                Wrappers.<RagMessage>lambdaQuery().orderByDesc(RagMessage::getCreateTime));
    }

    private List<ChatMessage> recentHistory(String conversationId) {
        List<RagMessage> rows = messageMapper.selectList(Wrappers.<RagMessage>lambdaQuery()
                .eq(RagMessage::getConversationId, conversationId)
                .orderByDesc(RagMessage::getId));
        List<ChatMessage> history = new ArrayList<>();
        for (int i = Math.min(rows.size(), 6) - 1; i >= 0; i--) {
            history.add(new ChatMessage(rows.get(i).getRole().toLowerCase(), rows.get(i).getContent()));
        }
        return history;
    }

    private RagMessage saveMessage(Long userId, String conversationId, String role,
                                   String content, String sources, String feedback) {
        RagMessage m = new RagMessage();
        m.setUserId(userId);
        m.setConversationId(conversationId);
        m.setRole(role);
        m.setContent(content);
        m.setSources(sources);
        m.setFeedback(feedback);
        messageMapper.insert(m);
        return m;
    }

    private String toJson(Object o) {
        try { return objectMapper.writeValueAsString(o); } catch (Exception e) { return "[]"; }
    }
}
```

`RagController` 追加：

```java
@SaCheckLogin
@PostMapping("/api/rag/chat")
public Result<ChatResponse> chat(@RequestBody ChatRequest req) {
    return Result.ok(chatService.chat(StpUtil.getLoginIdAsLong(), req));
}

@SaCheckLogin
@PostMapping(value = "/api/rag/chat", params = "stream=true", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public SseEmitter streamChat(@RequestBody ChatRequest req) {
    SseEmitter emitter = new SseEmitter(0L);
    executor.submit(() -> {
        try {
            chatService.streamAnswer(StpUtil.getLoginIdAsLong(), req, delta -> {
                try { emitter.send(SseEmitter.event().name("delta").data(delta)); }
                catch (java.io.IOException ignored) {}
            });
            emitter.send(SseEmitter.event().name("done").data("END"));
            emitter.complete();
        } catch (Exception e) {
            emitter.completeWithError(e);
        }
    });
    return emitter;
}

@SaCheckLogin
@PostMapping("/api/rag/messages/{id}/feedback")
public Result<Void> feedback(@PathVariable Long id, @RequestBody Map<String, String> body) {
    chatService.feedback(StpUtil.getLoginIdAsLong(), id, body.get("feedback"));
    return Result.ok(null);
}

@SaCheckLogin
@SaCheckRole("ADMIN")
@GetMapping("/api/rag/messages")
public Result<Page<RagMessage>> messages(@RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
    return Result.ok(chatService.pageMessages(page, size));
}
```

RagController 需补充 import：`cn.dev33.satoken.stp.StpUtil`、`org.springframework.http.MediaType`、`org.springframework.web.servlet.mvc.method.annotation.SseEmitter`、`com.baomidou.mybatisplus.extension.plugins.pagination.Page`、`com.meishan.agri.rag.entity.RagMessage`、`com.meishan.agri.rag.dto.ChatRequest/ChatResponse`，并声明字段 `private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newCachedThreadPool();`（演示级线程池，SSE 流式输出用）。测试以非流式 JSON 为准。

- [ ] **步骤 4：运行测试验证通过**

运行：`cd backend && mvn test -Dtest=RagTest`
预期：PASS

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: RAG 问答服务与离线降级"
```

---

### 任务 6：种子知识库与端到端回归

**文件：**
- 修改：`backend/src/main/resources/db/data.sql`（追加 6 篇 knowledge_doc 种子）
- 创建：`backend/src/main/java/com/meishan/agri/rag/runner/DocumentSeedRunner.java`
- 修改：`backend/src/test/java/com/meishan/agri/RagTest.java`（追加端到端断言）

- [ ] **步骤 1：编写失败的测试**

在 `RagTest` 追加：

```java
@Test
void seedDocsAreIngestedOnStartup() throws Exception {
    String token = adminToken();
    mockMvc.perform(get("/api/rag/documents").header("satoken", token))
            .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(6)));
    mockMvc.perform(post("/api/rag/chat").header("satoken", token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"question\":\"柑橘储存\"}"))
            .andExpect(jsonPath("$.data.sources.length()").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
}
```

- [ ] **步骤 2：运行测试验证失败**

运行：`cd backend && mvn test -Dtest=RagTest`
预期：FAIL（种子文档未入库）

- [ ] **步骤 3：实现种子文档与启动入库**

`data.sql` 追加（6 篇，覆盖 TECH/OPERATION/LIVE/POLICY/FAQ）：

```sql
INSERT INTO knowledge_doc (id, title, category, content, source, status) VALUES
(1, '眉山柑橘种植与采收保鲜', 'TECH', '丹棱桔橙应适时采收，避免早采影响糖酸比。采收后 24 小时内预冷至 4℃ 左右，储运过程保持通风、避免碰撞，可显著延长货架期。分级按丹棱标准：单果 80-90mm 为 A 级。', '眉山农业专家', 'ENABLED'),
(2, '东坡泡菜标准化生产', 'TECH', '东坡泡菜采用传统陶坛发酵，腌制期不低于 60 天。发酵车间保持恒温恒湿，原料青菜须经农残检测合格后入坛。出坛后分装、杀菌、质检合格方可出厂。', '东坡区农业农村局', 'ENABLED'),
(3, '农户零基础开店与商品上架', 'OPERATION', '开店三步：提交个人资质与土地证明，等待平台审核；审核通过后选择柑橘果园或泡菜基地模板；上传商品主图、规格与产地信息。主图建议白底实拍，标题包含品类+规格+卖点。', '平台运营', 'ENABLED'),
(4, '直播带货话术与场景搭建', 'LIVE', '产地直播建议在果园或泡菜车间开播，先展示采摘/发酵场景再上链接。话术结构：痛点开场-现场展示-规格价格-促单收尾。挂载商品使用直播专享价，突出产地直供。', '平台运营', 'ENABLED'),
(5, '眉山助农电商补贴申报', 'POLICY', '农户通过平台提交入驻资质后，可申报眉山市电商助农补贴。流程：平台初审-区县农业农村局复核-公示发放。补贴用于店铺装修、直播设备与冷链包装。', '眉山市商务局', 'ENABLED'),
(6, '商品售后与退换货常见问题', 'FAQ', '鲜活农产品签收后 24 小时内反馈质量问题可申请售后。泡菜类未开封可 7 天无理由退换。退款审核通过后原路退回。物流损坏请保留面单照片申请补发。', '平台客服', 'ENABLED');
```

`DocumentSeedRunner.java`：

```java
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
```

- [ ] **步骤 4：运行全量测试验证通过**

运行：`cd backend && mvn test`
预期：PASS（33 个既有 + RAG 新增全部通过）

- [ ] **步骤 5：Commit**

```bash
git add backend
git commit -m "feat: RAG 种子知识库与启动入库"
```

---

## 验收标准

1. `mvn test` 全绿（含既有 33 个测试与新增 RAG 测试）。
2. 管理后台可用接口：文档增删改查、消息列表；登录用户可对话（带来源）与反馈。
3. 无 DeepSeek Key / 断网时对话返回离线降级回答并保留来源。
4. 配置 `DEEPSEEK_API_KEY` 后，`POST /api/rag/chat` 返回真实模型回答；`stream=true` 走 SSE。
5. 启动日志出现"知识库启动入库完成"，知识库 6 篇种子文档自动切分。
