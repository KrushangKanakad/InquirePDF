package com.example.pdfqa.controller;

import com.example.pdfqa.service.SummarizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/summarize")
@RequiredArgsConstructor
@Slf4j
public class SummarizationController {

    private final SummarizationService summarizationService;

    @GetMapping
    public ResponseEntity<String> summarize(
            @RequestParam String docId,
            @RequestParam(required = false) String section,
            @RequestParam(defaultValue = "10") int topK) {
        try {
            String summary = summarizationService.getSummary(docId, section, topK);
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("Error while summarizing document: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Failed to summarize document.");
        }
    }
}
