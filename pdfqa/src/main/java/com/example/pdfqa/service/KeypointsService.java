package com.example.pdfqa.service;

import com.example.pdfqa.dto.KeypointsResponse;
import com.example.pdfqa.repository.InMemoryKeypointsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
public class KeypointsService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final InMemoryKeypointsRepository cache;

    public KeypointsResponse extractKeypoints(String docId) {
        return cache.get(docId).orElseGet(() -> {
            try {
                List<Document> documents = vectorStore.similaritySearch(
                        SearchRequest.builder().query(docId).topK(100).build());

                documents = documents.stream()
                        .filter(doc -> doc.getMetadata().getOrDefault("docId", "").equals(docId))
                        .collect(Collectors.toList());

                if (documents.isEmpty()) {
                    throw new IllegalArgumentException("No content found for docId: " + docId);
                }

                String fullContent = documents.stream()
                        .map(Document::getText)
                        .collect(Collectors.joining("\n"));

                String prompt = """
                        From the following text, extract:
                        1. Bullet-point key highlights.
                        2. 3-5 FAQs (with question and answer).
                        3. Any tables or figures mentioned.

                        Return JSON like:
                        {
                          "bulletPoints": [...],
                          "faqs": [{"question": "...", "answer": "..."}],
                          "tablesOrFigures": [...]
                        }

                        Text:
                        %s
                        """.formatted(fullContent);

                String llmRawOutput = chatClient.prompt().user(prompt).call().content();

                String jsonOnly = llmRawOutput.replaceAll("(?s)^.*?(\\{.*\\})\\s*$", "$1");

                KeypointsResponse response = objectMapper.readValue(jsonOnly, KeypointsResponse.class);
                cache.save(docId, response);
                return response;

            } catch (Exception e) {
                log.error("Failed to extract keypoints for docId: {}\nReason: {}", docId, e.getMessage(), e);
                throw new RuntimeException("Failed to extract keypoints for docId: " + docId, e);
            }
        });
    }
}
