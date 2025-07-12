package com.example.pdfqa.controller;

import com.example.pdfqa.dto.KeypointsResponse;
import com.example.pdfqa.service.KeypointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class KeypointsController {

    private final KeypointsService keypointsService;
    private final VectorStore vectorStore;

    @GetMapping("/keypoints")
    public ResponseEntity<KeypointsResponse> getKeypoints(@RequestParam String docId) {
        try {
            KeypointsResponse response = keypointsService.extractKeypoints(docId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/docs")
    public ResponseEntity<List<String>> listAllDocIds() {
        try {
            List<Document> allDocs = vectorStore.similaritySearch(
                    SearchRequest.builder().query("").topK(1000).build());

            List<String> docIds = allDocs.stream()
                    .map(d -> (String) d.getMetadata().get("docId"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            return ResponseEntity.ok(docIds);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
