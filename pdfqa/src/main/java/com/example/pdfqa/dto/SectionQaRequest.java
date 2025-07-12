package com.example.pdfqa.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SectionQaRequest {

    private String question;
    private String docId;
    private Integer pageNumber;
    private String sectionTitle;
    private int topK = 3;

    // Custom validation methods
    public boolean hasValidFilter() {
        return pageNumber != null || (sectionTitle != null && !sectionTitle.trim().isEmpty());
    }

    public boolean isValid() {
        return question != null && !question.trim().isEmpty()
                && docId != null && !docId.trim().isEmpty()
                && topK > 0
                && hasValidFilter();
    }

    public String getValidationError() {
        if (question == null || question.trim().isEmpty()) {
            return "Question is required";
        }
        if (docId == null || docId.trim().isEmpty()) {
            return "Document ID is required";
        }
        if (topK <= 0) {
            return "topK must be at least 1";
        }
        if (!hasValidFilter()) {
            return "Either pageNumber or sectionTitle must be provided";
        }
        return null;
    }
}