package com.meishan.agri.rag.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.meishan.agri.common.BizException;
import com.meishan.agri.rag.ai.ChatClient;
import com.meishan.agri.rag.dto.ChatMessage;
import com.meishan.agri.rag.dto.ChatRequest;
import com.meishan.agri.rag.dto.ChatResponse;
import com.meishan.agri.rag.entity.KnowledgeDoc;
import com.meishan.agri.rag.entity.RagMessage;
import com.meishan.agri.rag.mapper.KnowledgeDocMapper;
import com.meishan.agri.rag.mapper.RagMessageMapper;
import com.meishan.agri.rag.intent.IntentService;
import com.meishan.agri.rag.intent.IntentType;
import com.meishan.agri.rag.retrieval.RetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
    private final IntentService intentService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ChatResponse chat(Long userId, ChatRequest req) {
        IntentType intent = intentService.classify(req.getQuestion());
        if (intent == IntentType.GREETING) {
            return greeting(userId, req);
        }
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

    private ChatResponse greeting(Long userId, ChatRequest req) {
        String conversationId = req.getConversationId() == null || req.getConversationId().isBlank()
                ? UUID.randomUUID().toString() : req.getConversationId();
        saveMessage(userId, conversationId, "USER", req.getQuestion(), null, "NONE");
        String answer = "您好！欢迎来到眉山泡菜柑橘助农电商平台。我是您的助农智能客服，很高兴为您服务。请问有什么可以帮您？无论是选购商品、咨询售后，还是了解农户开店，都可以问我哦！";
        RagMessage saved = saveMessage(userId, conversationId, "ASSISTANT", answer, "[]", "NONE");
        ChatResponse resp = new ChatResponse();
        resp.setAnswer(answer);
        resp.setConversationId(conversationId);
        resp.setSources(List.of());
        resp.setOffline(false);
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

    public Page<RagMessage> pageMessages(int page, int size) {
        return messageMapper.selectPage(Page.of(page, size),
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
