package com.example.pdfqa.controller;

import com.example.pdfqa.dto.HighlightRequest;
import com.example.pdfqa.service.EmbeddingService;
import com.example.pdfqa.service.PdfHighlightService;
import com.example.pdfqa.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pdf")
@RequiredArgsConstructor
@Slf4j
public class PdfController {

    @Value("${pdf.storage-dir}")
    private String storageDir;

    private final PdfHighlightService pdfHighlightService;
    private final PdfService pdfService;
    private final EmbeddingService embeddingService;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadPdf(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Uploading PDF: {}", file.getOriginalFilename());

            Path pdfDir = Paths.get(storageDir);
            Files.createDirectories(pdfDir);

            Path target = pdfDir.resolve(file.getOriginalFilename());
            file.transferTo(target);
            log.info("PDF saved to: {}", target.toAbsolutePath());

            String text = pdfService.extractTextFromPdf(file);
            log.info("Extracted {} characters of text", text.length());

            List<String> chunks = pdfService.chunkText(text, 500);
            String docId = embeddingService.embedWithVersioning(chunks, file.getOriginalFilename());
            log.info("Created {} chunks for embedding; docId = {}", chunks.size(), docId);

            Map<String, String> body = Map.of(
                    "message", "PDF uploaded, saved to disk, and embedded successfully",
                    "docId", docId
            );

            return ResponseEntity.ok(body);

        } catch (IOException e) {
            log.error("IO error during PDF upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "IO error: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error during PDF upload: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Upload failed: " + e.getMessage()));
        }
    }

    @PostMapping("/highlight")
    public ResponseEntity<Resource> highlightPdf(@RequestBody HighlightRequest req) {
        log.info("Highlighting request for file: {} with {} highlights",
                req.getFilename(), req.getHighlights().size());

        try {
            File original = new File(storageDir, req.getFilename());
            if (!original.exists()) {
                log.error("Original PDF not found: {}", original.getAbsolutePath());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new InputStreamResource(
                                new ByteArrayInputStream("PDF file not found".getBytes())
                        ));
            }

            List<String> cleanedHighlights = preprocessHighlights(req.getHighlights());

            if (cleanedHighlights.isEmpty()) {
                log.warn("No valid highlight texts provided after preprocessing");
                return ResponseEntity.badRequest()
                        .body(new InputStreamResource(
                                new ByteArrayInputStream("No valid highlight texts provided".getBytes())
                        ));
            }

            log.info("Cleaned highlights for word matching: {}", cleanedHighlights);

            Path outputDir = Paths.get(storageDir);
            Files.createDirectories(outputDir);

            String outputPath = Paths.get(storageDir, "highlighted_" + req.getFilename()).toString();
            File highlighted = pdfHighlightService.highlightTextInPdf(
                    original,
                    cleanedHighlights,
                    outputPath
            );

            if (highlighted == null || !highlighted.exists()) {
                log.error("Failed to create highlighted PDF");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(new InputStreamResource(
                                new ByteArrayInputStream("Failed to create highlighted PDF".getBytes())
                        ));
            }

            log.info("Successfully created highlighted PDF: {}", highlighted.getAbsolutePath());

            InputStreamResource resource = new InputStreamResource(new FileInputStream(highlighted));
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(highlighted.length())
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"highlighted_" + req.getFilename() + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                    .header(HttpHeaders.PRAGMA, "no-cache")
                    .header(HttpHeaders.EXPIRES, "0")
                    .body(resource);

        } catch (IOException e) {
            log.error("IO error during PDF highlighting: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new InputStreamResource(
                            new ByteArrayInputStream(("IO error: " + e.getMessage()).getBytes())
                    ));
        } catch (Exception e) {
            log.error("Unexpected error during PDF highlighting: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new InputStreamResource(
                            new ByteArrayInputStream(("Highlighting failed: " + e.getMessage()).getBytes())
                    ));
        }
    }

    private List<String> preprocessHighlights(List<String> highlights) {
        return highlights.stream()
                .filter(text -> text != null && !text.trim().isEmpty())
                .map(this::cleanHighlightText)
                .filter(text -> !text.isEmpty())
                .filter(this::isValidHighlightText)
                .distinct() // Remove duplicates
                .collect(Collectors.toList());
    }

    private String cleanHighlightText(String text) {
        if (text == null) {
            return "";
        }

        return text
                .replaceAll("\\p{Cntrl}", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private boolean isValidHighlightText(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        if (text.length() < 1) {
            log.debug("Rejecting highlight text - too short: '{}'", text);
            return false;
        }

        if (text.length() > 100) {
            log.warn("Rejecting highlight text - too long: '{}'", text.substring(0, 20) + "...");
            return false;
        }

        if (!text.matches(".*[a-zA-Z0-9].*")) {
            log.debug("Rejecting highlight text - no alphanumeric characters: '{}'", text);
            return false;
        }

        return true;
    }
}
