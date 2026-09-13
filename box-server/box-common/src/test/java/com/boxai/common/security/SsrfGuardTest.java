package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SsrfGuardTest {

    @Test
    void blocksLocalhost() {
        assertThrows(BusinessException.class, () -> SsrfGuard.validateHttpUrl("http://127.0.0.1/secret"));
        assertThrows(BusinessException.class, () -> SsrfGuard.validateHttpUrl("http://localhost/admin"));
    }

    @Test
    void blocksPrivateNetwork() {
        assertThrows(BusinessException.class, () -> SsrfGuard.validateHttpUrl("http://192.168.1.8/internal"));
        assertThrows(BusinessException.class, () -> SsrfGuard.validateHttpUrl("http://10.0.0.4/metadata"));
    }

    @Test
    void blocksNonHttpScheme() {
        assertThrows(BusinessException.class, () -> SsrfGuard.validateHttpUrl("file:///etc/passwd"));
    }

    @Test
    void messageMentionsIntranet() {
        BusinessException ex = assertThrows(BusinessException.class,
                () -> SsrfGuard.validateHttpUrl("http://169.254.169.254/latest/meta-data"));
        assertTrue(ex.getMessage().contains("内网") || ex.getMessage().contains("本地"));
    }
}
