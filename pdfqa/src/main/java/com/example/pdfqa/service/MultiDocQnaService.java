package com.example.pdfqa.service;

import com.example.pdfqa.dto.MultiDocQaRequest;
import com.example.pdfqa.dto.MultiDocQaResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MultiDocQnaService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private static final int DEFAULT_TOP_K = 5;

    public MultiDocQaResponse askQuestionAcrossDocs(MultiDocQaRequest request) {
        return askAcrossMultipleDocs(request, DEFAULT_TOP_K);
    }

    private MultiDocQaResponse askAcrossMultipleDocs(MultiDocQaRequest request, int topK) {
        Map<String, List<String>> rawHitsByDoc = new HashMap<>();
        Map<String, String> answersByDoc = new HashMap<>();

        for (String docId : request.getDocIds()) {
            try {
                SearchRequest searchRequest = SearchRequest.builder()
                        .query(request.getQuestion())
                        .topK(topK)
                        .filterExpression("docId == '" + docId + "'")
                        .build();

                List<Document> hits = vectorStore.similaritySearch(searchRequest);

                if (hits.isEmpty()) {
                    throw new IllegalArgumentException("No content found for docId: " + docId);
                }

                List<String> hitTexts = hits.stream()
                        .map(Document::getText)
                        .collect(Collectors.toList());
                rawHitsByDoc.put(docId, hitTexts);

                String prompt = buildPrompt(request.getQuestion(), docId, hitTexts);

                ChatResponse resp = chatClient
                        .prompt(prompt)
                        .call()
                        .chatResponse();

                String answer = resp.getResult()
                        .getOutput()
                        .getText()
                        .trim();
                answersByDoc.put(docId, answer);

            } catch (Exception e) {
                e.printStackTrace();
                answersByDoc.put(docId, "Failed to generate answer: " + e.getMessage());
            }
        }

        MultiDocQaResponse response = new MultiDocQaResponse();
        response.setRawHitsByDoc(rawHitsByDoc);
        response.setAnswersByDocId(answersByDoc);

        if (request.isMergeAnswers()) {
            try {
                String mergedContext = rawHitsByDoc.values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.joining("\n\n===\n\n"));

                String mergedPrompt = buildPrompt(request.getQuestion(), "ALL_DOCS", List.of(mergedContext));
                ChatResponse mergedResp = chatClient
                        .prompt(mergedPrompt)
                        .call()
                        .chatResponse();

                response.setMergedAnswer(
                        mergedResp.getResult().getOutput().getText().trim()
                );
            } catch (Exception e) {
                e.printStackTrace();
                response.setMergedAnswer("Failed to generate merged answer: " + e.getMessage());
            }
        }

        return response;
    }

    private String buildPrompt(String question, String docId, List<String> contexts) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are a helpful assistant. Use the context below to answer the question.\n\n");
        sb.append("DOCUMENT: ").append(docId).append("\n\nCONTEXT:\n");
        for (int i = 0; i < contexts.size(); i++) {
            sb.append("Chunk ").append(i + 1).append(": ")
                    .append(contexts.get(i))
                    .append("\n\n");
        }
        sb.append("QUESTION: ").append(question).append("\n");
        sb.append("ANSWER:");
        return sb.toString();
    }
}
