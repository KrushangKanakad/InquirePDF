package com.example.pdfqa.service;

import com.example.pdfqa.repository.SummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SummarizationService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final SummaryRepository summaryRepository;

    public String getSummary(String docId, String section, int topK) {
        try {
            Optional<String> cached = summaryRepository.find(docId, section, topK);
            if (cached.isPresent()) {
                return cached.get();
            }

            List<Document> chunks = loadContent(docId, section, topK);
            if (chunks.isEmpty()) {
                return "No content found for summarization.";
            }

            String text = chunks.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n\n"));

            String prompt = "Summarize the following " +
                    (section != null ? "section" : "document") +
                    " using top " + topK + " chunks:\n\n" + text;

            ChatResponse resp = chatClient.prompt(prompt).call().chatResponse();
            String summary = resp.getResult().getOutput().getText().trim();

            summaryRepository.save(docId, section, topK, summary);
            return summary;

        } catch (Exception e) {
            log.error("Failed to generate summary for docId={}, section={}: {}", docId, section, e.getMessage(), e);
            return "Failed to generate summary.";
        }
    }

    private List<Document> loadContent(String docId, String section, int topK) {
        try {
            SearchRequest req = SearchRequest.builder()
                    .query(section != null ? section : "")
                    .topK(topK)
                    .build();

            return vectorStore.similaritySearch(req).stream()
                    .filter(d ->
                            docId.equals(d.getMetadata().get("docId")) &&
                                    (section == null || section.equals(d.getMetadata().get("section"))))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error loading content for summarization: docId={}, section={}", docId, section, e);
            return List.of();
        }
    }
}
