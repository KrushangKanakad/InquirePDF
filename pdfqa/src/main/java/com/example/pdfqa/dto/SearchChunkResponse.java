package com.example.pdfqa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchChunkResponse {
    private String content;
    private float score;
    private String docId;
}
