package com.boxai.knowledge.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtractedTextValidatorTest {

    @Test
    void shortTextIsNotTreatedAsNoise() {
        assertFalse(ExtractedTextValidator.looksLikeBinaryNoise("lblblblb"));
        assertFalse(ExtractedTextValidator.looksLikeBinaryNoise(null));
    }

    @Test
    void tikaStyleLblbRepeatsAreNoise() {
        assertTrue(ExtractedTextValidator.looksLikeBinaryNoise("lblblblblblblblblblblblblblblblblblb"));
    }

    @Test
    void normalDocumentTextIsAccepted() {
        assertFalse(ExtractedTextValidator.looksLikeBinaryNoise(
                "This is a normal knowledge document paragraph used for retrieval tests."));
    }
}
