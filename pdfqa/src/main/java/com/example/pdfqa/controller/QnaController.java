package com.example.pdfqa.controller;

import com.example.pdfqa.dto.QaResponse;
import com.example.pdfqa.dto.SectionQaRequest;
import com.example.pdfqa.service.QnaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qa")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @GetMapping
    public ResponseEntity<QaResponse> ask(
            @RequestParam String question,
            @RequestParam(defaultValue = "3") int topK) {
        try {
            QaResponse response = qnaService.answer(question, topK);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new QaResponse("Error occurred while answering the question."));
        }
    }

    @GetMapping("/ask-smart")
    public ResponseEntity<QaResponse> askSmart(
            @RequestParam String question,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) String sectionTitle,
            @RequestParam(defaultValue = "5") int topK) {
        try {
            QaResponse response = qnaService.answerSmart(question, pageNumber, sectionTitle, topK);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new QaResponse("Error occurred while answering smart question."));
        }
    }

    @PostMapping("/ask-smart")
    public ResponseEntity<QaResponse> askSmartPost(@RequestBody SmartQaRequest request) {
        try {
            if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new QaResponse("Question is required"));
            }

            QaResponse response = qnaService.answerSmart(
                    request.getQuestion(),
                    request.getPageNumber(),
                    request.getSectionTitle(),
                    request.getTopK() != null ? request.getTopK() : 5
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new QaResponse("Error occurred while answering smart question."));
        }
    }

    @PostMapping("/ask-by-section")
    public ResponseEntity<QaResponse> askBySection(@RequestBody SectionQaRequest request) {
        try {
            if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(new QaResponse("Question is required"));
            }

            QaResponse response = qnaService.answerBySection(
                    request.getQuestion(),
                    request.getDocId(),
                    request.getPageNumber(),
                    request.getSectionTitle(),
                    request.getTopK() > 0 ? request.getTopK() : 3
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new QaResponse("Error occurred while answering section-based question."));
        }
    }

    @PostMapping("/ask-by-section-params")
    public ResponseEntity<QaResponse> askBySectionParams(
            @RequestParam String question,
            @RequestParam(required = false) String docId,
            @RequestParam(required = false) Integer pageNumber,
            @RequestParam(required = false) String sectionTitle,
            @RequestParam(defaultValue = "3") int topK) {
        try {
            QaResponse response = qnaService.answerBySection(question, docId, pageNumber, sectionTitle, topK);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(new QaResponse("Error occurred while answering section-based question."));
        }
    }

    @GetMapping("/documents")
    public ResponseEntity<List<String>> getAvailableDocuments() {
        try {
            List<String> docIds = qnaService.getDocumentIds();
            return ResponseEntity.ok(docIds);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    public static class SmartQaRequest {
        private String question;
        private Integer pageNumber;
        private String sectionTitle;
        private Integer topK;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }

        public Integer getPageNumber() { return pageNumber; }
        public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

        public String getSectionTitle() { return sectionTitle; }
        public void setSectionTitle(String sectionTitle) { this.sectionTitle = sectionTitle; }

        public Integer getTopK() { return topK; }
        public void setTopK(Integer topK) { this.topK = topK; }
    }
}
