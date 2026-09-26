package com.boxai.knowledge.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KnowledgeQueryTermsTest {

    @Test
    void extractStripsQuestionFragmentsAndKeepsSubject() {
        List<String> terms = KnowledgeQueryTerms.extract("退款是什么");
        assertTrue(terms.contains("退款是什么"));
        assertTrue(terms.contains("退款"));
    }

    @Test
    void extractExpandsContactQueries() {
        List<String> terms = KnowledgeQueryTerms.extract("客服邮箱是什么");
        assertTrue(terms.contains("客服"));
        assertTrue(terms.contains("邮箱"));
        assertTrue(terms.contains("support@xingyun-tech.com"));
    }

    @Test
    void scoreContentBoostsEmailForContactIntent() {
        double score = KnowledgeQueryTerms.scoreContent("请联系 support@example.com", List.of("客服", "邮箱"));
        assertTrue(score > KnowledgeQueryTerms.scoreContent("请联系客服", List.of("客服", "邮箱")));
    }

    @Test
    void blankQueryYieldsNoTerms() {
        assertEquals(List.of(), KnowledgeQueryTerms.extract("   "));
        assertEquals(0D, KnowledgeQueryTerms.scoreContent("text", List.of()));
    }
}
