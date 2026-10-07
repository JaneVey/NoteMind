package com.notemind.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Redis 配置。
 *
 * <p><b>为什么需要这里的启动检查</b>
 *
 * <p>Spring Boot 的 Redis 自动配置采用<b>懒连接</b>：容器启动时只创建
 * {@code RedisConnectionFactory}，并不会真正建立连接。
 * 因此 {@code spring.data.redis.*} 配错（端口、密码、主机名）时，
 * <b>应用照样能正常启动</b>，直到第一次使用 Redis 才抛异常。
 * 这与本项目此前 MinIO 那个"看起来正常、其实从未连上"的问题属于同一类静默失效。
 *
 * <p>本项目 Redis 承担三个用途，任一失效都会造成实际影响：
 * <ol>
 *   <li>缓存用户 AI 配置（避免每次请求查库并解密 API Key）</li>
 *   <li>接口限流（公网部署后防止 API Key 被刷爆）</li>
 *   <li>登录失败计数（防爆破）</li>
 * </ol>
 *
 * <p>因此在此主动 ping 一次，把"连不上"这件事暴露在启动日志里。
 * 连接失败<b>不中断启动</b>（不影响不使用 Redis 的功能），但以 error 级别告警。
 *
 * <p>说明：开发环境 Redis 位于 Docker 的 {@code 127.0.0.1:6380}，
 * 因为宿主机 6379 被另一个项目的 Redis 3.0 Windows 服务占用（见 .env.example）。
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
