package com.example.pdfqa.dto;

import lombok.Data;
import java.util.List;

@Data
public class HighlightRequest {
    private String filename;
    private List<String> highlights;
}
