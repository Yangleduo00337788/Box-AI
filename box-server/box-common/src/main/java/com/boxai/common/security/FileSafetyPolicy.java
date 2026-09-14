package com.boxai.common.security;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class FileSafetyPolicy {

    public static final long MAX_SIZE_BYTES = 20L * 1024 * 1024;
    public static final long MAX_IMAGE_SIZE_BYTES = 8L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "md", "csv", "json", "png", "jpg", "jpeg", "svg");
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("png", "jpg", "jpeg", "svg");
    private static final int MAX_INLINE_SVG_CHARS = 65536;
    private static final Pattern SVG_EVENT_ATTR = Pattern.compile("on[a-z]+\\s*=", Pattern.CASE_INSENSITIVE);

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
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能超过 8MB");
        }
        String ext = requireAllowedExtension(originalName);
        if (!IMAGE_EXTENSIONS.contains(ext)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅支持 PNG / JPG / SVG 图片");
        }
        if ("svg".equals(ext)) {
            if (!looksLikeSafeSvg(bytes)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "SVG 内容无效或不安全");
            }
            if (contentType != null && !contentType.isBlank()) {
                String mime = contentType.toLowerCase(Locale.ROOT);
                if (!(mime.contains("svg") || mime.equals("image/svg+xml"))) {
                    throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持该 MIME 类型");
                }
            }
            return ext;
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
        if ("png".equals(ext) || "jpg".equals(ext) || "jpeg".equals(ext) || "pdf".equals(ext) || "svg".equals(ext)) {
            return false;
        }
        String sample = new String(bytes, 0, Math.min(bytes.length, 256), StandardCharsets.UTF_8)
                .trim()
                .toLowerCase(Locale.ROOT);
        return sample.startsWith("<!doctype html")
                || sample.startsWith("<html")
                || sample.startsWith("<script");
    }

    public static String sanitizeInlineSvg(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String text = raw.trim();
        if (text.length() > MAX_INLINE_SVG_CHARS) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SVG 代码过长");
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int start = lower.indexOf("<svg");
        if (start < 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入完整的 SVG 代码");
        }
        int endTag = lower.lastIndexOf("</svg>");
        String svg;
        if (endTag > start) {
            svg = text.substring(start, endTag + "</svg>".length());
        } else {
            int gt = text.indexOf('>', start);
            if (gt < 0) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入完整的 SVG 代码");
            }
            svg = text.substring(start, gt + 1);
            if (!svg.trim().endsWith("/>")) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请输入完整的 SVG 代码");
            }
        }
        String cleaned = stripUnsafeSvg(svg);
        if (!cleaned.toLowerCase(Locale.ROOT).contains("<svg")) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "SVG 内容无效或不安全");
        }
        return cleaned;
    }

    private static String stripUnsafeSvg(String svg) {
        return svg.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "")
                .replaceAll("(?is)<foreignObject\\b[^>]*>.*?</foreignObject>", "")
                .replaceAll("(?is)<iframe\\b[^>]*>.*?</iframe>", "")
                .replaceAll("(?is)<embed\\b[^>]*>.*?</embed>", "")
                .replaceAll("(?is)<object\\b[^>]*>.*?</object>", "")
                .replaceAll("(?i)javascript:", "")
                .replaceAll("(?i)\\s+on[a-z]+\\s*=\\s*(\"[^\"]*\"|'[^']*'|[^\\s>]+)", "");
    }

    private static boolean looksLikeSafeSvg(byte[] bytes) {
        String text = new String(bytes, StandardCharsets.UTF_8).trim();
        String lower = text.toLowerCase(Locale.ROOT);
        if (!lower.contains("<svg")) {
            return false;
        }
        return !lower.contains("<script")
                && !lower.contains("javascript:")
                && !lower.contains("foreignobject")
                && !lower.contains("<iframe")
                && !lower.contains("<embed")
                && !lower.contains("<object")
                && !SVG_EVENT_ATTR.matcher(lower).find();
    }
}
