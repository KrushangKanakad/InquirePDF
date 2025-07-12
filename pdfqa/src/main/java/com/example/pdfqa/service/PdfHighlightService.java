package com.example.pdfqa.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationTextMarkup;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
@Slf4j
public class PdfHighlightService {

    public File highlightTextInPdf(File originalPdf, List<String> texts, String outputPath) {
        try (PDDocument doc = PDDocument.load(originalPdf)) {
            log.info("Processing {} pages for highlighting", doc.getNumberOfPages());
            log.info("Number of texts to highlight: {}", texts.size());

            int totalMatches = 0;

            for (int pageIndex = 0; pageIndex < doc.getNumberOfPages(); pageIndex++) {
                PDPage page = doc.getPage(pageIndex);
                PDRectangle mediaBox = page.getMediaBox();
                float pageHeight = mediaBox.getHeight();

                log.info("Processing page {} with height {}", pageIndex + 1, pageHeight);

                for (String snippet : texts) {
                    if (snippet == null || snippet.trim().isEmpty()) {
                        log.warn("Skipping null or empty snippet");
                        continue;
                    }

                    String cleanSnippet = snippet.trim();
                    log.info("Looking for exact word: '{}'", cleanSnippet);

                    try {
                        PositionAwareStripper stripper = new PositionAwareStripper(cleanSnippet);
                        stripper.setStartPage(pageIndex + 1);
                        stripper.setEndPage(pageIndex + 1);
                        stripper.getText(doc);

                        List<PositionAwareStripper.Rect> matches = stripper.getMatches();
                        log.info("Found {} exact word matches for '{}' on page {}",
                                matches.size(), cleanSnippet, pageIndex + 1);
                        totalMatches += matches.size();

                        for (PositionAwareStripper.Rect r : matches) {
                            log.info("Original match coordinates: {}", r);

                            float horizontalPadding = Math.max(1.0f, r.w * 0.05f);
                            float verticalPadding = Math.max(1.0f, r.h * 0.1f);

                            float pdfX = Math.max(0, r.x - horizontalPadding);
                            float pdfY = Math.max(0, pageHeight - r.y - r.h - verticalPadding);
                            float pdfWidth = r.w + (1 * horizontalPadding);
                            float pdfHeight = r.h + (5 * verticalPadding);

                            pdfX = Math.min(pdfX, mediaBox.getWidth() - pdfWidth);
                            pdfY = Math.min(pdfY, pageHeight - pdfHeight);
                            pdfWidth = Math.min(pdfWidth, mediaBox.getWidth() - pdfX);
                            pdfHeight = Math.min(pdfHeight, pageHeight - pdfY);

                            log.info("Converted PDF coordinates: x={}, y={}, w={}, h={}",
                                    pdfX, pdfY, pdfWidth, pdfHeight);

                            PDAnnotationTextMarkup highlight = new PDAnnotationTextMarkup(PDAnnotationTextMarkup.SUB_TYPE_HIGHLIGHT);
                            highlight.setConstantOpacity(0.5f);
                            highlight.setColor(new PDColor(new float[]{1.0f, 1.0f, 0.0f}, PDDeviceRGB.INSTANCE));

                            PDRectangle rect = new PDRectangle(pdfX, pdfY, pdfWidth, pdfHeight);
                            highlight.setRectangle(rect);

                            float[] quadPoints = new float[]{
                                    pdfX, pdfY,
                                    pdfX + pdfWidth, pdfY,
                                    pdfX, pdfY + pdfHeight,
                                    pdfX + pdfWidth, pdfY + pdfHeight
                            };
                            highlight.setQuadPoints(quadPoints);

                            page.getAnnotations().add(highlight);
                            log.info("Successfully added highlight for word: '{}'", cleanSnippet);
                        }

                    } catch (Exception e) {
                        log.error("Error processing snippet '{}' on page {}: {}",
                                cleanSnippet, pageIndex + 1, e.getMessage(), e);
                    }
                }
            }

            log.info("Total exact word matches found across all pages: {}", totalMatches);

            File outputFile = new File(outputPath);
            doc.save(outputFile);
            log.info("Saved highlighted PDF to: {}", outputPath);
            return outputFile;

        } catch (IOException e) {
            log.error("Failed to highlight PDF due to IO issue: {}", e.getMessage(), e);
            return null;
        } catch (Exception e) {
            log.error("Unexpected error during highlighting: {}", e.getMessage(), e);
            throw new RuntimeException("Highlight processing failed", e);
        }
    }
}
