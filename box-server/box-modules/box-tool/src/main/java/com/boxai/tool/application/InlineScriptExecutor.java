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

@Component
public class InlineScriptExecutor {

    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public InlineScriptExecutor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String execute(String functionCode, String functionName, Map<String, Object> arguments, int timeoutMs) {
        Map<String, Object> args = arguments == null ? Map.of() : arguments;
        Callable<Object> task = () -> runScript(functionCode, functionName, args);
        try {
            Future<Object> future = executor.submit(task);
            Object result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            if (result == null) {
                return "";
            }
            if (result instanceof String text) {
                return text;
            }
            return objectMapper.writeValueAsString(result);
        } catch (TimeoutException ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "脚本执行超时");
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.EXECUTION_FAILED, "脚本执行失败: " + ex.getMessage());
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
