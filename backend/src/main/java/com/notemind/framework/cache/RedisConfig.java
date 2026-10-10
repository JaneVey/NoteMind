package com.notemind.framework.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis 配置。
 *
 * <p>Spring Boot 的 Redis 自动配置是懒连接：只创建 {@code RedisConnectionFactory}，
 * 配错 host/port/password 应用照样启动，直到第一次用到 Redis 才抛异常。
 * 所以在启动时主动 ping 一次，把连不上暴露在启动日志里；失败不中断启动。
 *
 * <p>开发环境 Redis 在 Docker 的 {@code 127.0.0.1:6380}（宿主机 6379 被占用，见 .env.example）。
 */
@Slf4j
@Configuration
public class RedisConfig {

    @Bean
    public ApplicationRunner redisConnectivityChecker(RedisConnectionFactory connectionFactory) {
        return args -> {
            try (RedisConnection connection = connectionFactory.getConnection()) {
                String pong = connection.ping();
                String version = "unknown";
                var info = connection.serverCommands().info("server");
                if (info != null && info.getProperty("redis_version") != null) {
                    version = info.getProperty("redis_version");
                }
                log.info("Redis 连接成功：PING={} version={}", pong, version);
            } catch (Exception e) {
                log.error("Redis 不可用，限流与 AI 配置缓存将无法工作。"
                        + "请确认 Redis 已启动，且 spring.data.redis 的 host/port/password 正确。原因: {}",
                        e.getMessage());
            }
        };
    }
}
