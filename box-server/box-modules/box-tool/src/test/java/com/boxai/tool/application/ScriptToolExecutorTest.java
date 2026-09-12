package com.boxai.tool.application;

import com.boxai.domain.tool.ToolFunctionConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScriptToolExecutorTest {

    @Test
    void executesJavascriptFunction() {
        ScriptToolExecutor executor = new ScriptToolExecutor(new InlineScriptExecutor(new ObjectMapper()));
        ToolFunctionConfig config = new ToolFunctionConfig();
        config.setFunctionName("execute");
        config.setFunctionCode("""
                function execute(args) {
                  return String(args.a + args.b);
                }
                """);
        config.setRuntime("JAVA_SCRIPT");
        config.setTimeoutMs(5000);

        String result = executor.execute(config, Map.of("a", 2, "b", 3));
        assertEquals("5", result);
    }
}
