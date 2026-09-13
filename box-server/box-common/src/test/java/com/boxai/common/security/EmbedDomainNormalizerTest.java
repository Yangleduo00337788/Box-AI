package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmbedDomainNormalizerTest {

    @Test
    void stripsSchemeAndPort() {
        assertEquals("chat.example.com", EmbedDomainNormalizer.normalize("https://Chat.Example.com:443/path"));
    }

    @Test
    void rejectsInvalidHost() {
        assertThrows(BusinessException.class, () -> EmbedDomainNormalizer.normalize("not a domain"));
    }
}
