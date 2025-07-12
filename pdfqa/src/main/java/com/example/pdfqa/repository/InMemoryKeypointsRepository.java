package com.example.pdfqa.repository;

import com.example.pdfqa.dto.KeypointsResponse;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryKeypointsRepository {

    private final Map<String, KeypointsResponse> cache = new ConcurrentHashMap<>();

    public Optional<KeypointsResponse> get(String docId) {
        return Optional.ofNullable(cache.get(docId));
    }

    public void save(String docId, KeypointsResponse response) {
        cache.put(docId, response);
    }
}
