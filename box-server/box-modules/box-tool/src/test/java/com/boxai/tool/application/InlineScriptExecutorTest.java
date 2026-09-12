package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InlineScriptExecutorTest {

    private final InlineScriptExecutor executor = new InlineScriptExecutor(new ObjectMapper());

    @Test
    void executesSimpleScript() {
        String result = executor.execute(
                "function execute(args) { return String(args.value + 1); }",
                "execute",
                Map.of("value", 2),
                5000);
        assertEquals("3", result);
    }

    @Test
    void rejectsForbiddenJavaAccess() {
        BusinessException ex = assertThrows(BusinessException.class, () -> executor.execute(
                "function execute(args) { return Java.type('java.lang.System').getProperty('user.home'); }",
                "execute",
                Map.of(),
                5000));
        assertEquals(ErrorCode.SCRIPT_SECURITY_VIOLATION, ex.getCode());
    }

    @Test
    void timesOutLongRunningScript() {
        BusinessException ex = assertThrows(BusinessException.class, () -> executor.execute(
                "function execute(args) { while(true) {} return 'ok'; }",
                "execute",
                Map.of(),
                200));
        assertEquals(ErrorCode.SCRIPT_TIMEOUT, ex.getCode());
    }
}
