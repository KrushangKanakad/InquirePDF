package com.example.pdfqa.service;

import com.example.pdfqa.dto.SearchChunkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final VectorStore vectorStore;

    public List<SearchChunkResponse> searchChunks(String query) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .query(query)
                    .topK(5)
                    .build();

            List<Document> results = vectorStore.similaritySearch(request);

            return results.stream()
                    .map(doc -> new SearchChunkResponse(
                            doc.getText(),
                            doc.getScore() != null ? doc.getScore().floatValue() : 0f,
                            doc.getMetadata().getOrDefault("docId", "unknown").toString()
                    ))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error during semantic chunk search for query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }
}
