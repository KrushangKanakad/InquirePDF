package com.example.pdfqa.service;

import com.example.pdfqa.dto.QaResponse;
import com.example.pdfqa.dto.QaResponseWithHits;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder.Op;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QnaService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    private Set<String> availableDocIds = null;
    private long lastDocIdCacheUpdate = 0;
    private static final long CACHE_DURATION = 5 * 60 * 1000;

    public QaResponse answerSmart(String question, Integer pageNumber, String sectionTitle, int topK) {
        log.info("Smart answer for: {}", question);
        String docId = detectDocId(question);
        log.info("Using docId: {}", docId);
        return answerBySection(question, docId, pageNumber, sectionTitle, topK);
    }

    private String detectDocId(String question) {
        if (question == null || question.isBlank()) return null;
        Set<String> ids = getAvailableDocIds();
        for (String d : ids) {
            if (question.toLowerCase().contains(d.toLowerCase())) return d;
        }
        return ids.isEmpty() ? null : ids.iterator().next();
    }

    private Set<String> getAvailableDocIds() {
        long now = System.currentTimeMillis();
        if (availableDocIds == null || now - lastDocIdCacheUpdate > CACHE_DURATION) {
            refreshCache();
            lastDocIdCacheUpdate = now;
        }
        return availableDocIds != null ? availableDocIds : Collections.emptySet();
    }

    private void refreshCache() {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query("*").topK(1000).build());

            availableDocIds = docs.stream()
                    .map(d -> d.getMetadata().getOrDefault("docId", d.getMetadata().get("source")).toString())
                    .collect(Collectors.toSet());

            log.info("Cached IDs: {}", availableDocIds);

        } catch (Exception e) {
            log.error("Unexpected error refreshing doc ID cache", e);
            availableDocIds = Collections.emptySet();
        }
    }

    public QaResponse answer(String question, int topK) {
        QaResponseWithHits resp = answerWithHits(question, topK);
        return new QaResponse(resp.getAnswer());
    }

    public QaResponseWithHits answerWithHits(String question, int topK) {
        try {
            List<Document> hits = vectorStore.similaritySearch(
                    SearchRequest.builder().query(question).topK(topK).build());

            List<String> texts = hits.stream().map(Document::getText).toList();
            String prompt = buildPrompt(question, texts);

            ChatResponse chat = chatClient.prompt(prompt).call().chatResponse();
            return new QaResponseWithHits(chat.getResult().getOutput().getText().trim(), texts);

        } catch (Exception e) {
            log.error("Unexpected error during Q&A with hits", e);
            return new QaResponseWithHits("Unexpected error occurred.", List.of());
        }
    }

    public QaResponse answerBySection(String question, String docId, Integer pageNumber,
                                      String sectionTitle, int topK) {
        QaResponseWithHits resp = answerBySectionWithHits(question, docId, pageNumber, sectionTitle, topK);
        return new QaResponse(resp.getAnswer());
    }

    public QaResponseWithHits answerBySectionWithHits(String question, String docId,
                                                      Integer pageNumber, String sectionTitle, int topK) {
        log.info("askBySection: docId={}, page={}, section={}, topK={}", docId, pageNumber, sectionTitle, topK);

        try {
            var sb = SearchRequest.builder().query(question).topK(topK);
            FilterExpressionBuilder feb = new FilterExpressionBuilder();
            Op combined = null;

            if (docId != null && !docId.isBlank()) combined = feb.eq("docId", docId.trim());
            if (pageNumber != null) combined = (combined == null)
                    ? feb.eq("pageNumber", pageNumber)
                    : feb.and(combined, feb.eq("pageNumber", pageNumber));
            if (sectionTitle != null && !sectionTitle.isBlank()) combined = (combined == null)
                    ? feb.eq("sectionTitle", sectionTitle.trim())
                    : feb.and(combined, feb.eq("sectionTitle", sectionTitle.trim()));

            if (combined != null) sb.filterExpression(combined.build());

            List<Document> hits = vectorStore.similaritySearch(sb.build());

            if (hits.isEmpty()) {
                log.warn("No hits with filters, retrying without filters");
                hits = vectorStore.similaritySearch(
                        SearchRequest.builder().query(question).topK(topK).build());
            }

            if (hits.isEmpty()) {
                String msg = (docId != null)
                        ? "No relevant information for document: " + docId
                        : "No relevant information found for your query.";
                return new QaResponseWithHits(msg, List.of());
            }

            List<String> texts = hits.stream().map(Document::getText).toList();
            String prompt = buildPrompt(question, texts);

            ChatResponse chat = chatClient.prompt(prompt).call().chatResponse();
            return new QaResponseWithHits(chat.getResult().getOutput().getText().trim(), texts);

        } catch (Exception e) {
            log.error("Unexpected error during section-level Q&A", e);
            return new QaResponseWithHits("Unexpected error occurred while answering.", List.of());
        }
    }

    private String buildPrompt(String question, List<String> contexts) {
        var sb = new StringBuilder();
        sb.append("You are a helpful assistant. Context below.\n\n");
        for (int i = 0; i < contexts.size(); i++) {
            sb.append("Chunk ").append(i + 1).append(": ").append(contexts.get(i)).append("\n\n");
        }
        sb.append("QUESTION: ").append(question).append("\nANSWER:");
        return sb.toString();
    }

    public List<String> getDocumentIds() {
        return new ArrayList<>(getAvailableDocIds());
    }

    public Map<String, Object> debugDocumentMetadata() {
        try {
            List<Document> docs = vectorStore.similaritySearch(
                    SearchRequest.builder().query("test").topK(5).build());

            Map<String, Object> info = new HashMap<>();
            info.put("totalDocs", docs.size());

            if (!docs.isEmpty()) {
                info.put("metadataKeys", docs.get(0).getMetadata().keySet());
            }

            return info;

        } catch (Exception e) {
            log.error("Unexpected error during debug metadata retrieval", e);
            return Map.of("error", e.getMessage());
        }
    }
}