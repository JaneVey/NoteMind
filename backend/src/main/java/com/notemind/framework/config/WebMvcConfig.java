package com.notemind.framework.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p>不要在这里自定义 {@code ObjectMapper} Bean：它会覆盖 Spring Boot 的自动配置，
 * 让 {@code spring.jackson.*} 以及其它 starter 注册的模块（如 Spring AI 需要的序列化模块）失效。
 * 个别字段要特定格式，用 {@code @JsonFormat} 注解在字段上控制。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
