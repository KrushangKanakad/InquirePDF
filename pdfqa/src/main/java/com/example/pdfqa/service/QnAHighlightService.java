package com.example.pdfqa.service;

import com.example.pdfqa.dto.QaResponseWithHits;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class QnAHighlightService {

    @Autowired
    private PdfHighlightService pdfHighlightService;

    public File highlightQnAAnswer(File originalPdf, QaResponseWithHits qnaResponse, String outputPath) {
        try {
            List<String> textsToHighlight = extractTextSegmentsForHighlighting(qnaResponse);

            log.info("Extracted {} text segments to highlight", textsToHighlight.size());
            textsToHighlight.forEach(text -> log.info("Text to highlight: '{}'", text));

            return pdfHighlightService.highlightTextInPdf(originalPdf, textsToHighlight, outputPath);

        } catch (Exception e) {
            if (e instanceof java.io.IOException) {
                log.error("IO error during PDF highlighting", e);
            } else {
                log.error("Unexpected error during PDF QnA highlighting", e);
            }
            return null;
        }
    }

    private List<String> extractTextSegmentsForHighlighting(QaResponseWithHits qnaResponse) {
        List<String> textsToHighlight = new ArrayList<>();
        String answer = qnaResponse.getAnswer();
        List<String> hits = qnaResponse.getHits();

        List<String> keyPhrases = extractKeyPhrasesFromAnswer(answer);

        for (String keyPhrase : keyPhrases) {
            for (String hit : hits) {
                if (hit.toLowerCase().contains(keyPhrase.toLowerCase())) {
                    textsToHighlight.addAll(extractRelevantSentences(hit, keyPhrase));
                }
            }
        }

        if (textsToHighlight.isEmpty()) {
            textsToHighlight.addAll(getCompleteRelevantHits(hits));
        }

        return textsToHighlight.stream()
                .distinct()
                .filter(text -> text.trim().length() > 10)
                .toList();
    }

    private List<String> extractKeyPhrasesFromAnswer(String answer) {
        List<String> keyPhrases = new ArrayList<>();
        String[] sentences = answer.split("[.!?]+");

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() > 20) {
                keyPhrases.addAll(extractImportantTerms(sentence));
            }
        }

        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.length() > 30 && sentence.length() < 150) {
                keyPhrases.add(sentence);
            }
        }

        return keyPhrases;
    }

    private List<String> extractImportantTerms(String sentence) {
        List<String> terms = new ArrayList<>();

        Pattern capitalPattern = Pattern.compile("\\b[A-Z][a-z]*(?:\\s+[A-Z][a-z]*)*\\b");
        Matcher matcher = capitalPattern.matcher(sentence);

        while (matcher.find()) {
            String term = matcher.group().trim();
            if (term.length() > 3) {
                terms.add(term);
            }
        }

        String[] keyWords = {"process", "photolithography", "photoresist", "UV light", "mask", "oxide", "etching", "doping"};

        for (String keyword : keyWords) {
            if (sentence.toLowerCase().contains(keyword.toLowerCase())) {
                String phrase = extractPhraseAroundKeyword(sentence, keyword);
                if (phrase != null) {
                    terms.add(phrase);
                }
            }
        }

        return terms;
    }

    private String extractPhraseAroundKeyword(String sentence, String keyword) {
        int index = sentence.toLowerCase().indexOf(keyword.toLowerCase());
        if (index == -1) return null;

        int start = Math.max(0, index - 50);
        int end = Math.min(sentence.length(), index + keyword.length() + 50);
        String phrase = sentence.substring(start, end).trim();

        if (start > 0) {
            int spaceIndex = phrase.indexOf(' ');
            if (spaceIndex > 0) phrase = phrase.substring(spaceIndex + 1);
        }

        if (end < sentence.length()) {
            int lastSpaceIndex = phrase.lastIndexOf(' ');
            if (lastSpaceIndex > 0) phrase = phrase.substring(0, lastSpaceIndex);
        }

        return phrase.length() > 10 ? phrase : null;
    }

    private List<String> extractRelevantSentences(String hit, String keyPhrase) {
        List<String> sentences = new ArrayList<>();

        String cleanHit = hit.replaceAll("[\\r\\n]+", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("\\u0000", " ")
                .trim();

        int phraseIndex = cleanHit.toLowerCase().indexOf(keyPhrase.toLowerCase());
        if (phraseIndex == -1) return sentences;

        int start = Math.max(0, phraseIndex - 200);
        int end = Math.min(cleanHit.length(), phraseIndex + keyPhrase.length() + 200);
        String context = cleanHit.substring(start, end);

        String[] contextSentences = context.split("[.!?]+");
        for (String sentence : contextSentences) {
            sentence = sentence.trim();
            if (sentence.length() > 20 &&
                    (sentence.toLowerCase().contains(keyPhrase.toLowerCase()) ||
                            sentence.toLowerCase().contains("photolithography") ||
                            sentence.toLowerCase().contains("photoresist") ||
                            sentence.toLowerCase().contains("oxide"))) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    private List<String> getCompleteRelevantHits(List<String> hits) {
        List<String> relevantHits = new ArrayList<>();

        for (String hit : hits) {
            String cleanHit = hit.replaceAll("[\\r\\n]+", " ")
                    .replaceAll("\\s+", " ")
                    .replaceAll("\\u0000", " ")
                    .trim();

            if (cleanHit.toLowerCase().matches(".*(photolithography|photoresist|mask|uv light|oxide).*")) {
                relevantHits.addAll(splitIntoChunks(cleanHit, 150));
            }
        }

        return relevantHits;
    }

    private List<String> splitIntoChunks(String text, int maxLength) {
        List<String> chunks = new ArrayList<>();

        if (text.length() <= maxLength) {
            chunks.add(text);
            return chunks;
        }

        String[] sentences = text.split("[.!?]+");
        StringBuilder currentChunk = new StringBuilder();

        for (String sentence : sentences) {
            sentence = sentence.trim();

            if (currentChunk.length() + sentence.length() + 1 <= maxLength) {
                if (currentChunk.length() > 0) currentChunk.append(". ");
                currentChunk.append(sentence);
            } else {
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString());
                    currentChunk = new StringBuilder(sentence);
                } else {
                    chunks.add(sentence);
                }
            }
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString());
        }

        return chunks;
    }
}
