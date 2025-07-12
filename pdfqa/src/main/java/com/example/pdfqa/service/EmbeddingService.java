package com.example.pdfqa.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class EmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final VectorStore vectorStore;
    private final PdfService pdfService;

    private long storedCount = 0;

    public String embedWithVersioning(List<String> chunks, String filename) {
        try {
            String docId = UUID.randomUUID().toString();

            List<Document> existingDocs = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("versioning lookup " + filename)
                            .topK(1000)
                            .build()
            );

            Set<String> existingHashes = new HashSet<>();
            for (Document doc : existingDocs) {
                if (filename.equals(doc.getMetadata().get("filename"))) {
                    existingHashes.add(String.valueOf(doc.getMetadata().get("hash")));
                }
            }

            List<String> newChunks = new ArrayList<>();
            List<String> newHashes = new ArrayList<>();
            for (String chunk : chunks) {
                String hash = pdfService.hashChunk(chunk);
                if (!existingHashes.contains(hash)) {
                    newChunks.add(chunk);
                    newHashes.add(hash);
                }
            }

            if (newChunks.isEmpty()) {
                return docId;
            }

            List<float[]> rawEmbeddings = embeddingModel.embed(newChunks);

            List<Document> docsToStore = IntStream.range(0, newChunks.size())
                    .mapToObj(i -> {
                        Document d = new Document(newChunks.get(i));
                        d.getMetadata().put("filename", filename);
                        d.getMetadata().put("hash", newHashes.get(i));
                        d.getMetadata().put("docId", docId);

                        float[] vec = rawEmbeddings.get(i);
                        List<Double> embedding = new ArrayList<>(vec.length);
                        for (float f : vec) embedding.add((double) f);
                        d.getMetadata().put("embedding", embedding);

                        return d;
                    })
                    .collect(Collectors.toList());

            vectorStore.add(docsToStore);
            storedCount += docsToStore.size();

            return docId;

        } catch (Exception e) {
            e.printStackTrace();
            return "error-doc-id";
        }
    }

    public long getStoredCount() {
        return storedCount;
    }
}
