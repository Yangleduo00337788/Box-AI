package com.boxai.agent.application;

import com.boxai.agent.api.plugin.PluginCatalogAssetVO;
import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.FileSafetyPolicy;
import com.boxai.domain.storage.ObjectStorage;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PluginCatalogAssetApplicationService {

    private static final String PREFIX = "plugin-catalog/";
    private static final long MAX_BYTES = 20L * 1024 * 1024;

    private final ObjectStorage objectStorage;

    public PluginCatalogAssetApplicationService(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    public PluginCatalogAssetVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传文件");
        }
        byte[] bytes = readBytes(file);
        String originalName = FileSafetyPolicy.sanitizeFileName(
                file.getOriginalFilename() == null ? "asset.bin" : file.getOriginalFilename());
        FileSafetyPolicy.validate(originalName, file.getContentType(), file.getSize(), bytes);
        String storageKey = PREFIX + UUID.randomUUID() + "/" + originalName;
        String bucket = objectStorage.defaultBucket();
        try {
            objectStorage.put(bucket, storageKey, new ByteArrayInputStream(bytes), bytes.length, file.getContentType());
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "插件资源上传失败");
        }
        return new PluginCatalogAssetVO(storageKey, originalName, bytes.length, file.getContentType());
    }

    public byte[] readAsset(String storageKey) {
        if (storageKey == null || storageKey.isBlank() || !storageKey.startsWith(PREFIX)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的插件资源路径");
        }
        try (InputStream in = objectStorage.get(objectStorage.defaultBucket(), storageKey)) {
            return in.readAllBytes();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "插件资源不存在或无法读取");
        }
    }

    public String resolveSkillInstructions(String skillMdStorageKey, String skillPackageStorageKey) {
        if (skillMdStorageKey != null && !skillMdStorageKey.isBlank()) {
            byte[] bytes = readAsset(skillMdStorageKey);
            return new String(bytes, StandardCharsets.UTF_8).trim();
        }
        if (skillPackageStorageKey != null && !skillPackageStorageKey.isBlank()) {
            return extractSkillMarkdownFromZip(readAsset(skillPackageStorageKey));
        }
        return null;
    }

    static String extractSkillMarkdownFromZip(byte[] zipBytes) {
        String skillMd = null;
        String fallbackMd = null;
        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                if (!name.toLowerCase(Locale.ROOT).endsWith(".md")) {
                    continue;
                }
                byte[] content = readEntry(zis);
                String text = new String(content, StandardCharsets.UTF_8).trim();
                if (text.isBlank()) {
                    continue;
                }
                String base = name.contains("/") ? name.substring(name.lastIndexOf('/') + 1) : name;
                if ("SKILL.md".equalsIgnoreCase(base)) {
                    skillMd = text;
                    break;
                }
                if (fallbackMd == null) {
                    fallbackMd = text;
                }
            }
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 压缩包无法解析，请包含 SKILL.md");
        }
        String resolved = skillMd != null ? skillMd : fallbackMd;
        if (resolved == null || resolved.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Skill 压缩包内未找到 .md 文件");
        }
        return resolved;
    }

    private static byte[] readEntry(InputStream in) throws java.io.IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        return out.toByteArray();
    }

    private static byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
        }
    }
}
