package com.boxai.knowledge.support;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Normalizes user queries for keyword retrieval (especially Chinese FAQ-style questions).
 */
public final class KnowledgeQueryTerms {

    private static final Pattern CJK_OR_ALNUM = Pattern.compile("[\\u4e00-\\u9fff]+|[a-zA-Z0-9][a-zA-Z0-9._-]*");
    private static final List<String> QUESTION_FRAGMENTS = List.of(
            "多少钱", "多少", "是什么", "什么是", "如何", "怎么", "怎样", "哪些", "哪个",
            "有没有", "是否", "能不能", "可不可以", "吗", "呢", "啊");

    /** Maps user phrasing to tokens that appear in docs (often not as one contiguous string). */
    private static final List<String> CONTACT_QUERY_MARKERS = List.of(
            "客服邮箱", "客服邮件", "客服", "邮箱", "邮件", "e-mail", "email");
    private static final List<String> HOTLINE_QUERY_MARKERS = List.of("热线", "电话", "联系电话", "客服电话");

    private KnowledgeQueryTerms() {
    }

    public static String normalize(String query) {
        if (query == null) {
            return "";
        }
        String text = query.trim();
        if (text.isEmpty()) {
            return "";
        }
        return text.replaceAll("[?？!！。．.,，、；;：:\"'\"''\\s]+", " ").trim();
    }

    public static List<String> extract(String query) {
        LinkedHashSet<String> terms = new LinkedHashSet<>();
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return List.of();
        }
        terms.add(normalized);

        String subject = normalized;
        for (String fragment : QUESTION_FRAGMENTS) {
            subject = subject.replace(fragment, " ");
        }
        subject = subject.trim().replaceAll("\\s+", " ");
        if (!subject.isBlank() && !subject.equals(normalized)) {
            terms.add(subject);
        }

        collectRuns(normalized, terms);
        if (!subject.isBlank()) {
            collectRuns(subject, terms);
        }
        expandContactAndHotlineTerms(normalized, terms);
        return new ArrayList<>(terms);
    }

    private static void expandContactAndHotlineTerms(String normalized, LinkedHashSet<String> terms) {
        String lower = normalized.toLowerCase(Locale.ROOT);
        boolean contactQuery = CONTACT_QUERY_MARKERS.stream().anyMatch(marker -> lower.contains(marker.toLowerCase(Locale.ROOT)));
        if (contactQuery) {
            terms.add("客服");
            terms.add("邮箱");
            terms.add("support@xingyun-tech.com");
            terms.add("support@xingyun");
            terms.add("support@");
        }
        boolean hotlineQuery = HOTLINE_QUERY_MARKERS.stream().anyMatch(marker -> normalized.contains(marker));
        if (hotlineQuery) {
            terms.add("热线");
            terms.add("400-880-2024");
            terms.add("400-880");
        }
    }

    private static void collectRuns(String text, LinkedHashSet<String> terms) {
        Matcher matcher = CJK_OR_ALNUM.matcher(text);
        while (matcher.find()) {
            String run = matcher.group();
            if (run == null || run.isBlank()) {
                continue;
            }
            if (run.length() >= 2 || run.chars().allMatch(ch -> ch >= 0x4e00 && ch <= 0x9fff)) {
                terms.add(run);
            }
            if (run.chars().allMatch(ch -> ch >= 0x4e00 && ch <= 0x9fff) && run.length() >= 3) {
                for (int len = 2; len <= Math.min(4, run.length()); len++) {
                    for (int i = 0; i + len <= run.length(); i++) {
                        terms.add(run.substring(i, i + len));
                    }
                }
            }
        }
    }

    public static double scoreContent(String content, List<String> terms) {
        if (content == null || content.isBlank() || terms == null || terms.isEmpty()) {
            return 0D;
        }
        String lower = content.toLowerCase(Locale.ROOT);
        double score = 0D;
        for (String term : terms) {
            if (term == null || term.isBlank()) {
                continue;
            }
            String needle = term.toLowerCase(Locale.ROOT);
            if (lower.contains(needle)) {
                score += Math.max(needle.length(), 1);
            }
        }
        if (containsContactIntent(terms) && lower.contains("@")) {
            score += 6D;
        }
        return score;
    }

    private static boolean containsContactIntent(List<String> terms) {
        for (String term : terms) {
            if (term == null) {
                continue;
            }
            String t = term.toLowerCase(Locale.ROOT);
            if (t.contains("邮箱") || t.contains("客服") || t.contains("support@") || t.contains("邮件")) {
                return true;
            }
        }
        return false;
    }
}
