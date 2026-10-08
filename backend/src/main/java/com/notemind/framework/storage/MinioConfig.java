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
 * <p><b>变更记录（2026-10-07）——修复一个静默失效的 bug</b>
 *
 * <p>原实现把 bucket 初始化写在 {@code @PostConstruct} 方法里，并在其中调用本类的
 * {@code @Bean minioClient()} 方法：
 *
 * <pre>
 *   &#64;PostConstruct
 *   public void initBucket() {
 *       MinioClient client = minioClient();   // ← 自调用
 *   }
 * </pre>
 *
 * <p>由于 {@code @Configuration} 类被 CGLIB 代理，这种自调用会尝试在 {@code minioConfig}
 * 这个 Bean 尚在创建过程中再次获取它，抛出：
 * <pre>
 *   Error creating bean with name 'minioConfig': Requested bean is currently in creation
 * </pre>
 * 而该异常又被 {@code catch (Exception e)} 吞掉（只打了一条 warn），
 * 结果是 <b>bucket 从来没有被创建或校验过，启动日志看起来却"正常"</b>。
 * 上传功能要到真正上传时才会报错，属于典型的静默失效。
 *
 * <p>修复方式：改用 {@link ApplicationRunner}，它由 Spring 在<b>容器刷新完成后</b>调用，
 * 且 {@link MinioClient} 通过方法参数注入（而非自调用），彻底避开循环依赖。
 *
 * <p>另外把失败日志级别从 warn 提升为 error 并带上 endpoint，
 * 使"MinIO 没起来"这件事在启动日志里一眼可见。
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
     * 启动后确保 bucket 存在。容器刷新完成后执行，客户端通过参数注入。
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
                // 不中断启动：MinIO 不可用时其它功能仍可使用，但文件上传必然失败，
                // 因此用 error 级别并给出可操作的提示。
                log.error("MinIO 不可用（endpoint={}），文件上传功能将无法使用。"
                        + "请确认 MinIO 已启动且 minio.access-key / secret-key 正确。原因: {}",
                        endpoint, e.getMessage());
            }
        };
    }
}
