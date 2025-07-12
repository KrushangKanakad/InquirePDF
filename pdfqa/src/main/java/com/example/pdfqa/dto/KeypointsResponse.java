package com.example.pdfqa.dto;

import lombok.Data;

import java.util.List;

@Data
public class KeypointsResponse {
    private List<String> bulletPoints;
    private List<Faq> faqs;
    private List<String> tablesOrFigures;

    @Data
    public static class Faq {
        private String question;
        private String answer;
    }
}
