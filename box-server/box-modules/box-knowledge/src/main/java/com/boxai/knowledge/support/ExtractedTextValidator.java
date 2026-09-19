package com.boxai.knowledge.support;

public final class ExtractedTextValidator {

    private ExtractedTextValidator() {
    }

    /**
     * Detects Tika-style PNG/binary noise (e.g. repeated "lblb") mistaken as document text.
     */
    public static boolean looksLikeBinaryNoise(String text) {
        if (text == null) {
            return false;
        }
        String trimmed = text.trim();
        if (trimmed.length() < 32) {
            return false;
        }
        int sampleLength = Math.min(trimmed.length(), 256);
        String sample = trimmed.substring(0, sampleLength);
        long lbPairs = 0;
        for (int i = 0; i < sample.length() - 1; i++) {
            if (sample.charAt(i) == 'l' && sample.charAt(i + 1) == 'b') {
                lbPairs++;
            }
        }
        if (lbPairs >= 12) {
            return true;
        }
        int[] counts = new int[128];
        int asciiLetters = 0;
        for (int i = 0; i < sample.length(); i++) {
            char ch = sample.charAt(i);
            if (ch < 128) {
                counts[ch]++;
                if ((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
                    asciiLetters++;
                }
            }
        }
        if (asciiLetters < sample.length() * 0.9) {
            return false;
        }
        int dominant = 0;
        for (int count : counts) {
            dominant = Math.max(dominant, count);
        }
        return dominant >= sample.length() * 0.45;
    }
}
