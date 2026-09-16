package com.boxai.common.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmbedDomainTxtLookupTest {

    @Test
    void matchesBareTokenAndPrefixedRecord() {
        assertTrue(EmbedDomainTxtLookup.matches("abc123", "abc123"));
        assertTrue(EmbedDomainTxtLookup.matches("\"abc123\"", "abc123"));
        assertTrue(EmbedDomainTxtLookup.matches("box-verify=abc123", "abc123"));
        assertFalse(EmbedDomainTxtLookup.matches("other=abc123", "abc123"));
        assertFalse(EmbedDomainTxtLookup.matches("prefix-abc123-suffix", "abc123"));
    }
}
