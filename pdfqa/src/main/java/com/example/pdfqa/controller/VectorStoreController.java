package com.example.pdfqa.controller;

import com.example.pdfqa.service.EmbeddingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/store")
@RequiredArgsConstructor
public class VectorStoreController {

    private final EmbeddingService embeddingService;

    @GetMapping("/count")
    public ResponseEntity<Map<String, Long>> count() {
        try {
            long cnt = embeddingService.getStoredCount();
            return ResponseEntity.ok(Map.of("count", cnt));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("count", -1L));
        }
    }
}
