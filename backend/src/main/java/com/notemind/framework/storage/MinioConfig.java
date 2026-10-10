package com.notemind.framework.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置。
 *
 * <p>bucket 校验必须放在 {@link ApplicationRunner} 里、客户端用方法参数注入：
 * {@code @Configuration} 类被 CGLIB 代理，在 {@code @PostConstruct} 里自调用本类的
 * {@code @Bean} 方法会报 "Requested bean is currently in creation"，
 * 一旦这个异常被吞掉，bucket 就没被创建过而启动日志看起来正常，要等到上传才炸。
 *
 * <p>失败日志带 endpoint 且用 error 级别，让"MinIO 没起来"在启动日志里一眼可见。
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Getter
    @Value("${minio.bucket}")
    private String bucket;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 启动后确保 bucket 存在。
     */
    @Bean
    public ApplicationRunner minioBucketInitializer(MinioClient minioClient) {
        return args -> {
            try {
                boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build());
                if (exists) {
                    log.info("MinIO bucket '{}' 已存在（endpoint={}）", bucket, endpoint);
                } else {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                    log.info("MinIO bucket '{}' 创建成功（endpoint={}）", bucket, endpoint);
                }
            } catch (Exception e) {
                // 不中断启动：其它功能仍可用，但文件上传必然失败
                log.error("MinIO 不可用（endpoint={}），文件上传功能将无法使用。"
                        + "请确认 MinIO 已启动且 minio.access-key / secret-key 正确。原因: {}",
                        endpoint, e.getMessage());
            }
        };
    }
}
