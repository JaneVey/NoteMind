package com.notemind.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置。
 *
 * <p><b>变更记录（2026-10-07，随 Spring Boot 4 升级）</b>
 *
 * <p>此处原先自定义了一个注册 {@code JavaTimeModule} 的 {@code ObjectMapper} Bean，
 * 现已移除。原因有三，且第三条才是关键：
 *
 * <ol>
 *   <li>Spring Boot 4 迁移到 <b>Jackson 3</b>（包名由 {@code com.fasterxml.jackson}
 *       变为 {@code tools.jackson}），{@code com.fasterxml.jackson.datatype.jsr310.JavaTimeModule}
 *       已不存在，Java 8 日期时间支持已内置到 databind；</li>
 *   <li>Spring Boot 默认已关闭 {@code WRITE_DATES_AS_TIMESTAMPS}，
 *       日期时间会序列化为 ISO-8601 字符串；</li>
 *   <li><b>最关键</b>：自定义 {@code ObjectMapper} Bean 会<b>覆盖 Spring Boot 的自动配置</b>，
 *       导致 {@code spring.jackson.*} 配置项、以及其它 starter 注册的模块
 *       （例如 Spring AI 需要的序列化模块）全部失效。这类"手写全家桶"是常见陷阱。</li>
 * </ol>
 *
 * <p>若某个字段需要特定格式，用 {@code @JsonFormat(pattern = "...")} 注解在字段上控制即可，
 * 无需替换全局 ObjectMapper。
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
}
