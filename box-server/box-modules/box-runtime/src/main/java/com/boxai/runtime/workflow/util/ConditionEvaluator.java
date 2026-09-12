package com.boxai.runtime.workflow.util;

import java.util.Objects;

public final class ConditionEvaluator {

    private ConditionEvaluator() {
    }

    public static boolean evaluate(Object actual, String operator, String expected) {
        String actualText = actual == null ? null : String.valueOf(actual);
        return switch (operator == null ? "equals" : operator.toLowerCase()) {
            case "notequals", "not_equals", "!=" -> !Objects.equals(actualText, expected);
            case "contains" -> actualText != null && expected != null && actualText.contains(expected);
            case "empty" -> actualText == null || actualText.isBlank();
            case "notempty", "not_empty" -> actualText != null && !actualText.isBlank();
            default -> Objects.equals(actualText, expected);
        };
    }
}
