package com.example.pdfqa.repository;

import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemorySummaryRepository implements SummaryRepository {

    private final Map<String, String> store = new ConcurrentHashMap<>();

    private String key(String docId, String section, int topK) {
        return docId + "::" + (section != null ? section : "full") + "::" + topK;
    }

    @Override
    public Optional<String> find(String docId, String section, int topK) {
        return Optional.ofNullable(store.get(key(docId, section, topK)));
    }

    @Override
    public void save(String docId, String section, int topK, String summary) {
        store.put(key(docId, section, topK), summary);
    }
}

