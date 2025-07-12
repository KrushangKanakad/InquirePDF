package com.example.pdfqa.repository;

import java.util.Optional;

public interface SummaryRepository {
    Optional<String> find(String docId, String section, int topK);
    void save(String docId, String section, int topK, String summary);
}

