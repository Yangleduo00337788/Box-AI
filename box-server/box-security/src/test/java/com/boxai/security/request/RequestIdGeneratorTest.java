package com.boxai.security.request;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RequestIdGeneratorTest {

    @Test
    void generateUsesReqPrefix() {
        assertThat(RequestIdGenerator.generate()).startsWith("req_");
    }

    @Test
    void resolveKeepsValidIncomingId() {
        assertThat(RequestIdGenerator.resolve("req_client123")).isEqualTo("req_client123");
    }

    @Test
    void resolveGeneratesWhenBlank() {
        assertThat(RequestIdGenerator.resolve("   ")).startsWith("req_");
    }
}
