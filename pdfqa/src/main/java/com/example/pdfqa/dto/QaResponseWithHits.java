package com.example.pdfqa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaResponseWithHits {
    private String answer;
    private List<String> hits;
}