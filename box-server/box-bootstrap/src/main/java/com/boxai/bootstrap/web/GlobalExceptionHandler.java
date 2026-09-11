package com.boxai.bootstrap.web;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 直接写 JSON 到 response，避免流式接口 Accept: text/event-stream 时
 * 内容协商失败变成 401 / HttpMediaTypeNotAcceptableException。
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler(BusinessException.class)
    public void handleBusiness(BusinessException e, HttpServletResponse response) throws IOException {
        writeJson(response, resolveHttpStatus(e.getCode()), Result.failure(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class, ConstraintViolationException.class})
    public void handleValidation(HttpServletResponse response) throws IOException {
        writeJson(response, HttpStatus.BAD_REQUEST, Result.failure(ErrorCode.BAD_REQUEST, "请求参数校验失败"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public void handleNotFound(HttpServletResponse response) throws IOException {
        writeJson(response, HttpStatus.NOT_FOUND, Result.failure(ErrorCode.NOT_FOUND, "资源不存在"));
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public void handleNotAcceptable(HttpServletResponse response) throws IOException {
        writeJson(response, HttpStatus.BAD_REQUEST, Result.failure(ErrorCode.BAD_REQUEST, "请求无法完成"));
    }

    @ExceptionHandler(Exception.class)
    public void handleException(Exception e, HttpServletResponse response) throws IOException {
        log.error("Unhandled error", e);
        writeJson(response, HttpStatus.INTERNAL_SERVER_ERROR, Result.failure(ErrorCode.INTERNAL_ERROR, "服务器内部错误"));
    }

    private HttpStatus resolveHttpStatus(int code) {
        if (code == ErrorCode.UNAUTHORIZED) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (code == ErrorCode.FORBIDDEN
                || code == ErrorCode.QUOTA_EXCEEDED
                || code == ErrorCode.TENANT_DISABLED) {
            return HttpStatus.FORBIDDEN;
        }
        if (code == ErrorCode.NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private void writeJson(HttpServletResponse response, HttpStatus status, Result<Void> body) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.resetBuffer();
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), body);
        response.flushBuffer();
    }
}
