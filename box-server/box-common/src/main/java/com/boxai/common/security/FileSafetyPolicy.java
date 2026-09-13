package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

public final class FileSafetyPolicy {

    public static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_IMAGE_SIZE_BYTES = 2L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "md", "csv", "json", "png", "jpg", "jpeg");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg");

    private FileSafetyPolicy() {
    }

    public static String sanitizeFileName(String originalName) {
        String name = originalName == null ? "document.txt" : originalName.replace('\\', '/');
        int slash = name.lastIndexOf('/');
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        name = name.replace("..", "_").trim();
        if (name.isEmpty() || name.startsWith(".")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件名无效");
        }
        return name;
    }

    public static String requireAllowedExtension(String fileName) {
        String sanitized = sanitizeFileName(fileName);
        int dot = sanitized.lastIndexOf('.');
        if (dot < 0 || dot == sanitized.length() - 1) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型");
        }
        String ext = sanitized.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的文件类型: " + ext);
        }
        return ext;
    }

    public static void validate(String originalName, String contentType, long size, byte[] bytes) {
        if (size <= 0 || (bytes != null && bytes.length == 0)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传文件");
        }
        if (size > MAX_SIZE_BYTES || (bytes != null && bytes.length > MAX_SIZE_BYTES)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件不能超过 20MB");
        }
        String ext = requireAllowedExtension(originalName);
        if (looksLikeExecutable(bytes) || looksLikeMarkup(bytes, ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "文件内容不安全，已拒绝上传");
        }
        if (contentType != null && !contentType.isBlank()) {
            String mime = contentType.toLowerCase(Locale.ROOT);
            if (mime.contains("javascript") || mime.contains("html") || mime.contains("svg")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持该 MIME 类型");
            }
        }
    }

    public static String validateImage(String originalName, String contentType, long size, byte[] bytes) {
        if (size <= 0 || bytes == null || bytes.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传图片");
        }
        if (size > MAX_IMAGE_SIZE_BYTES || bytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能超过 2MB");
        }
        String ext = requireAllowedExtension(originalName);
        if (!IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 PNG / JPG 图片");
        }
        if (!looksLikeImage(bytes, ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片内容无效");
        }
        if (contentType != null && !contentType.isBlank()) {
            String mime = contentType.toLowerCase(Locale.ROOT);
            if (!(mime.startsWith("image/png") || mime.startsWith("image/jpeg") || mime.startsWith("image/jpg"))) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持该 MIME 类型");
            }
        }
        return ext;
    }

    private static boolean looksLikeImage(byte[] bytes, String ext) {
        if (bytes.length < 3) {
            return false;
        }
        if ("png".equals(ext)) {
            return bytes.length >= 4
                    && bytes[0] == (byte) 0x89
                    && bytes[1] == 0x50
                    && bytes[2] == 0x4E
                    && bytes[3] == 0x47;
        }
        return bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8 && bytes[2] == (byte) 0xFF;
    }

    private static boolean looksLikeExecutable(byte[] bytes) {
        if (bytes == null || bytes.length < 2) {
            return false;
        }
        if (bytes[0] == 'M' && bytes[1] == 'Z') {
            return true;
        }
        String head = new String(bytes, 0, Math.min(bytes.length, 8), StandardCharsets.US_ASCII);
        return head.startsWith("#!") || head.startsWith("\u007fELF");
    }

    private static boolean looksLikeMarkup(byte[] bytes, String ext) {
        if (bytes == null || bytes.length == 0) {
            return false;
        }
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "pdf".equals(ext)) {
            return false;
        }
        String sample = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8)
                .trim()
                .toLowerCase(Locale.ROOT);
        return sample.startsWith("<!doctype html")
                || sample.startsWith("<html")
                || sample.startsWith("<svg")
                || sample.startsWith("<script");
    }
}
