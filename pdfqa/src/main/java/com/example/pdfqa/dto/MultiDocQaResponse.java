package com.example.pdfqa.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class MultiDocQaResponse {
    private Map<String, List<String>> rawHitsByDoc;
    private Map<String, String>       answersByDocId;
    private String                    mergedAnswer;      // when mergeAnswers=true
}
