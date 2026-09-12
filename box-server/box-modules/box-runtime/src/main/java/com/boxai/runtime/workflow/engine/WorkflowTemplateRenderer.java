package com.boxai.runtime.workflow.engine;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class WorkflowTemplateRenderer {

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([\\w.-]+)\\s*}}");

    public String render(String template, Map<String, Object> variables) {
        if (template == null) {
            return "";
        }
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            Object value = resolveVariable(key, variables);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(value == null ? "" : String.valueOf(value)));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private Object resolveVariable(String key, Map<String, Object> variables) {
        if (variables.containsKey(key)) {
            return variables.get(key);
        }
        if (key == null || !key.contains(".")) {
            return null;
        }
        String[] parts = key.split("\\.");
        Object current = variables.get(parts[0]);
        for (int i = 1; i < parts.length && current != null; i++) {
            if (current instanceof Map<?, ?> map) {
                current = map.get(parts[i]);
            } else {
                return null;
            }
        }
        return current;
    }
}
