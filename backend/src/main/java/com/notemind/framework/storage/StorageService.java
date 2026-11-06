package com.notemind.framework.storage;

import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

/**
 * 图片上传。对象键按调用方给的目录前缀组织；public/ 前缀由 bucket 策略开放匿名读。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StorageService {

    private static final long MAX_IMAGE_SIZE = 1024 * 1024;

    private static final Map<String, String> CONTENT_TYPE_EXT = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/webp", ".webp");

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket}")
    private String bucket;

    public String uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "文件不能为空");
        }
        String extension = CONTENT_TYPE_EXT.get(file.getContentType());
        if (extension == null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "仅支持 PNG / JPEG / WebP 图片");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "图片不能超过 1MB");
        }

        String key = folder + "/" + UUID.randomUUID() + extension;
        try (InputStream input = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(key)
                    .stream(input, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
        } catch (Exception e) {
            log.error("图片上传失败 key={}", key, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "图片上传失败");
        }
        return publicUrl(key);
    }

    private String publicUrl(String key) {
        String base = endpoint.endsWith("/")
                ? endpoint.substring(0, endpoint.length() - 1)
                : endpoint;
        return base + "/" + bucket + "/" + key;
    }
}
