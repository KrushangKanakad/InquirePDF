package com.example.pdfqa.dto;

import lombok.Data;
import java.util.List;

@Data
public class MultiDocQaRequest {
    private String question;
    private List<String> docIds;
    private boolean mergeAnswers; // optional: true = return 1 merged answer
}
