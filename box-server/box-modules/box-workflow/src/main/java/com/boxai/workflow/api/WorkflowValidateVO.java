package com.boxai.workflow.api;

import java.util.List;

public record WorkflowValidateVO(
        boolean valid,
        List<String> errors
) {
}
