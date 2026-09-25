package com.boxai.user.application;

import com.boxai.common.exception.BusinessException;
import com.boxai.common.exception.ErrorCode;
import com.boxai.common.security.FileSafetyPolicy;
import com.boxai.domain.storage.ObjectStorage;
import com.boxai.user.api.ImageAssetVO;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class PublicImageAssetService {

    private static final Pattern FILE_NAME = Pattern.compile("^[0-9a-fA-F-]{36}\\.(png|jpg|jpeg|svg)$");
    private static final Set<String> IMAGE_EXT = Set.of("png", "jpg", "jpeg", "svg");

    private final ObjectStorage objectStorage;

    public PublicImageAssetService(ObjectStorage objectStorage) {
        this.objectStorage = objectStorage;
    }

    public ImageAssetVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传图片");
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "读取上传文件失败");
        }
        String originalName = FileSafetyPolicy.sanitizeFileName(
                file.getOriginalFilename() == null ? "avatar.png" : file.getOriginalFilename());
        String ext = FileSafetyPolicy.validateImage(originalName, file.getContentType(), file.getSize(), bytes);
        String fileName = UUID.randomUUID() + "." + ext;
        String key = objectKey(fileName);
        objectStorage.put(objectStorage.defaultBucket(), key, new ByteArrayInputStream(bytes), bytes.length, contentType(ext));
        return new ImageAssetVO("/api/v1/public-assets/" + fileName);
    }

    public LoadedImage load(String fileName) {
        if (fileName == null || !FILE_NAME.matcher(fileName).matches()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        if (!IMAGE_EXT.contains(ext)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
        try {
            InputStream stream = openObject(objectKey(fileName));
            return new LoadedImage(stream, contentType(ext));
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
        }
    }

    private InputStream openObject(String key) {
        String bucket = objectStorage.defaultBucket();
        try {
            return objectStorage.get(objectStorage.activeBackend(), bucket, key);
        } catch (Exception ignored) {
            // 历史资源在 MinIO
        }
        if (!"MINIO".equalsIgnoreCase(objectStorage.activeBackend())) {
            return objectStorage.get("MINIO", bucket, key);
        }
        throw new BusinessException(ErrorCode.NOT_FOUND, "图片不存在");
    }

    private static String objectKey(String fileName) {
        return "public-assets/" + fileName;
    }

    private static String contentType(String ext) {
        if ("png".equals(ext)) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if ("svg".equals(ext)) {
            return "image/svg+xml";
        }
        return MediaType.IMAGE_JPEG_VALUE;
    }

    public record LoadedImage(InputStream stream, String contentType) {
    }
}
