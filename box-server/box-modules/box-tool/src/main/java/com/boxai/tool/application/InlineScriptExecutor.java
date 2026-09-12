package com.boxai.tool.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;
import org.springframework.stereotype.Component;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

@Component
public class InlineScriptExecutor {

    private static final Pattern[] FORBIDDEN_PATTERNS = {
            Pattern.compile("(?i)Java\\.type"),
            Pattern.compile("(?i)importPackage"),
            Pattern.compile("(?i)importClass"),
            Pattern.compile("(?i)load\\s*\\("),
            Pattern.compile("(?i)loadWithNewGlobal"),
            Pattern.compile("(?i)Packages\\."),
            Pattern.compile("(?i)java\\.lang"),
            Pattern.compile("(?i)java\\.io"),
            Pattern.compile("(?i)java\\.net"),
            Pattern.compile("(?i)java\\.nio"),
            Pattern.compile("(?i)javax\\.script"),
            Pattern.compile("(?i)ProcessBuilder"),
            Pattern.compile("(?i)Runtime\\.getRuntime"),
            Pattern.compile("(?i)Files\\."),
            Pattern.compile("(?i)FileReader"),
            Pattern.compile("(?i)FileWriter"),
            Pattern.compile("(?i)Socket\\s*\\("),
            Pattern.compile("(?i)URL\\s*\\("),
            Pattern.compile("(?i)fetch\\s*\\("),
            Pattern.compile("(?i)XMLHttpRequest"),
    };

    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public InlineScriptExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String execute(String functionCode, String functionName, Map<String, Object> arguments, int timeoutMs) {
        assertScriptSafe(functionCode);
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        Callable<Object> task = () -> runScript(functionCode, functionName, args);
        Future<Object> future = executor.submit(task);
        try {
            Object result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (result == null) {
                return "";
            }
            if (result instanceof String text) {
                return text;
            }
            return objectMapper.writeValueAsString(result);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new BusinessException(ErrorCode.SCRIPT_TIMEOUT, "脚本执行超时");
        } catch (BusinessException ex) {
            future.cancel(true);
            throw ex;
        } catch (Exception ex) {
            future.cancel(true);
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "脚本执行失败: " + ex.getMessage());
        }
    }

    void assertScriptSafe(String functionCode) {
        if (functionCode == null || functionCode.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "脚本内容不能为空");
        }
        for (Pattern pattern : FORBIDDEN_PATTERNS) {
            if (pattern.matcher(functionCode).find()) {
                throw new BusinessException(ErrorCode.SCRIPT_SECURITY_VIOLATION, "脚本包含禁止的操作: " + pattern.pattern());
            }
        }
    }

    private Object runScript(String functionCode, String functionName, Map<String, Object> arguments) {
        try {
            ScriptEngine engine = new NashornScriptEngineFactory().getScriptEngine("--no-java");
            Bindings bindings = engine.createBindings();
            bindings.put("args", arguments);
            engine.eval(functionCode, bindings);
            String entry = functionName == null || functionName.isBlank() ? "execute" : functionName.trim();
            Object result = engine.eval(entry + "(args)", bindings);
            if (result == null && bindings.get(entry) == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "脚本需定义 function " + entry + "(args)");
            }
            return result;
        } catch (BusinessException ex) {
            throw ex;
        } catch (ScriptException ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "脚本语法错误: " + ex.getMessage());
        }
    }
}
