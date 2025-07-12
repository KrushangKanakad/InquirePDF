package com.example.pdfqa.controller;

import com.example.pdfqa.dto.MultiDocQaRequest;
import com.example.pdfqa.dto.MultiDocQaResponse;
import com.example.pdfqa.service.MultiDocQnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
public class MultiDocQnaController {

    private final MultiDocQnaService multiDocQnaService;

    @PostMapping("/multi-doc-ask")
    public ResponseEntity<MultiDocQaResponse> askAcrossMultipleDocs(@RequestBody MultiDocQaRequest request) {
        try {
            MultiDocQaResponse response = multiDocQnaService.askQuestionAcrossDocs(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}
