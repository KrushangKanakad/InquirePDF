package com.example.pdfqa.controller;

import com.example.pdfqa.dto.QaResponseWithHits;
import com.example.pdfqa.service.QnaService;
import com.example.pdfqa.service.QnAHighlightService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/highlight")
@Slf4j
public class HighlightController {

    @Autowired
    private QnAHighlightService qnAHighlightService;

    @Autowired
    private QnaService qnaService;

    @PostMapping("/question")
    public ResponseEntity<Resource> highlightByQuestion(
            @RequestParam("file") MultipartFile file,
            @RequestParam("question") String question,
            @RequestParam(defaultValue = "3") int topK) {

        try {
            log.info("Received request to highlight PDF based on question: {}", question);

            QaResponseWithHits qnaResponse = qnaService.answerWithHits(question, topK);

            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempFilePath = Paths.get(tempDir, "temp_" + System.currentTimeMillis() + "_" + file.getOriginalFilename());
            Files.write(tempFilePath, file.getBytes());
            File tempFile = tempFilePath.toFile();

            String outputPath = tempDir + File.separator + "highlighted_" + System.currentTimeMillis() + ".pdf";

            File highlightedPdf = qnAHighlightService.highlightQnAAnswer(tempFile, qnaResponse, outputPath);

            if (highlightedPdf == null || !highlightedPdf.exists()) {
                log.error("Failed to create highlighted PDF");
                return ResponseEntity.internalServerError().build();
            }

            tempFile.delete();

            Resource resource = new FileSystemResource(highlightedPdf);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"highlighted_" + file.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Error processing PDF highlighting request", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/qna")
    public ResponseEntity<Resource> highlightQnAInPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("answer") String answer,
            @RequestParam("hits") List<String> hits) {

        try {
            log.info("Received request to highlight QnA answer in PDF");
            log.info("Answer: {}", answer);
            log.info("Number of hits: {}", hits.size());

            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempFilePath = Paths.get(tempDir, "temp_" + System.currentTimeMillis() + "_" + file.getOriginalFilename());
            Files.write(tempFilePath, file.getBytes());
            File tempFile = tempFilePath.toFile();

            QaResponseWithHits qnaResponse = new QaResponseWithHits(answer, hits);

            String outputPath = tempDir + File.separator + "highlighted_" + System.currentTimeMillis() + ".pdf";

            File highlightedPdf = qnAHighlightService.highlightQnAAnswer(tempFile, qnaResponse, outputPath);

            if (highlightedPdf == null || !highlightedPdf.exists()) {
                log.error("Failed to create highlighted PDF");
                return ResponseEntity.internalServerError().build();
            }

            tempFile.delete();

            Resource resource = new FileSystemResource(highlightedPdf);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"highlighted_" + file.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Error processing PDF highlighting request", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/qna-json")
    public ResponseEntity<Resource> highlightQnAInPdfJson(
            @RequestParam("file") MultipartFile file,
            @RequestBody QaResponseWithHits qnaResponse) {

        try {
            log.info("Received JSON request to highlight QnA answer in PDF");
            log.info("Answer: {}", qnaResponse.getAnswer());
            log.info("Number of hits: {}", qnaResponse.getHits().size());

            String tempDir = System.getProperty("java.io.tmpdir");
            Path tempFilePath = Paths.get(tempDir, "temp_" + System.currentTimeMillis() + "_" + file.getOriginalFilename());
            Files.write(tempFilePath, file.getBytes());
            File tempFile = tempFilePath.toFile();

            String outputPath = tempDir + File.separator + "highlighted_" + System.currentTimeMillis() + ".pdf";

            File highlightedPdf = qnAHighlightService.highlightQnAAnswer(tempFile, qnaResponse, outputPath);

            if (highlightedPdf == null || !highlightedPdf.exists()) {
                log.error("Failed to create highlighted PDF");
                return ResponseEntity.internalServerError().build();
            }

            tempFile.delete();

            Resource resource = new FileSystemResource(highlightedPdf);

            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"highlighted_" + file.getOriginalFilename() + "\"")
                    .body(resource);

        } catch (IOException e) {
            log.error("Error processing PDF highlighting request", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}