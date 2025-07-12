package com.example.pdfqa.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PositionAwareStripper extends PDFTextStripper {

    private final String target;
    private final Pattern wordPattern;
    private final List<Rect> matches = new ArrayList<>();
    private final List<TextPosition> allTextPositions = new ArrayList<>();
    private final StringBuilder allText = new StringBuilder();
    private final List<Integer> charToPositionMap = new ArrayList<>();

    public PositionAwareStripper(String target) throws IOException {
        super();
        this.target = target.trim();
        this.wordPattern = Pattern.compile("\\b" + Pattern.quote(this.target) + "\\b",
                Pattern.CASE_INSENSITIVE);
        setSortByPosition(true);
    }

    @Override
    protected void writeString(String str, List<TextPosition> textPositions) throws IOException {
        for (int i = 0; i < str.length() && i < textPositions.size(); i++) {
            char ch = str.charAt(i);

            if (Character.isWhitespace(ch)) {
                allText.append(' ');
            } else {
                allText.append(ch);
            }

            charToPositionMap.add(allTextPositions.size() + i);
        }

        allTextPositions.addAll(textPositions);
        super.writeString(str, textPositions);
    }

    @Override
    protected void endPage(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
        super.endPage(page);

        processPageMatches();

        allText.setLength(0);
        allTextPositions.clear();
        charToPositionMap.clear();
    }

    private void processPageMatches() {
        if (allText.length() == 0 || allTextPositions.isEmpty()) {
            return;
        }

        String pageText = allText.toString();
        Matcher matcher = wordPattern.matcher(pageText);

        while (matcher.find()) {
            int startCharIndex = matcher.start();
            int endCharIndex = matcher.end();

            try {
                Rect rect = calculateBoundingBox(startCharIndex, endCharIndex);
                if (rect != null) {
                    matches.add(rect);
                    log.debug("Found word match '{}' at: {}", target, rect);
                }
            } catch (Exception e) {
                System.err.println("Error calculating bounding box for match at index " +
                        startCharIndex + ": " + e.getMessage());
            }
        }
    }

    private Rect calculateBoundingBox(int startCharIndex, int endCharIndex) {
        if (startCharIndex < 0 || endCharIndex > charToPositionMap.size() ||
                startCharIndex >= endCharIndex) {
            return null;
        }

        int startPosIndex = charToPositionMap.get(startCharIndex);
        int endPosIndex = (endCharIndex - 1) < charToPositionMap.size() ?
                charToPositionMap.get(endCharIndex - 1) : allTextPositions.size() - 1;

        if (startPosIndex >= allTextPositions.size() || endPosIndex >= allTextPositions.size() ||
                startPosIndex < 0 || endPosIndex < 0) {
            return null;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        for (int i = startPosIndex; i <= endPosIndex && i < allTextPositions.size(); i++) {
            TextPosition tp = allTextPositions.get(i);

            float x = tp.getXDirAdj();
            float y = tp.getYDirAdj();
            float width = tp.getWidthDirAdj();
            float height = tp.getHeightDir();

            float textTop = y;
            float textBottom = y - height;

            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x + width);
            minY = Math.min(minY, textBottom);
            maxY = Math.max(maxY, textTop);
        }

        if (minX == Float.MAX_VALUE || minY == Float.MAX_VALUE) {
            return null;
        }

        return new Rect(minX, minY, maxX - minX, maxY - minY);
    }

    @Override
    public String getText(PDDocument document) throws IOException {
        matches.clear();
        allText.setLength(0);
        allTextPositions.clear();
        charToPositionMap.clear();

        return super.getText(document);
    }

    public List<Rect> getMatches() {
        return new ArrayList<>(matches);
    }

    public static class Rect {
        public final float x, y, w, h;

        public Rect(float x, float y, float w, float h) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
        }

        @Override
        public String toString() {
            return String.format("Rect[x=%.2f, y=%.2f, w=%.2f, h=%.2f]", x, y, w, h);
        }
    }
}