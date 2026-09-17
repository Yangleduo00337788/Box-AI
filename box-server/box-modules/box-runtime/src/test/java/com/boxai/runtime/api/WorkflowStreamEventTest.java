package com.boxai.runtime.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class WorkflowStreamEventTest {

    @Test
    void nodeEndAllowsNullErrorMessage() {
        assertDoesNotThrow(() -> WorkflowStreamEvent.nodeEnd("start", "Start", "SUCCEEDED", 1L, null, null));
    }
}
