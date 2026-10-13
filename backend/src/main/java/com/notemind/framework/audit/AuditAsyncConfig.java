package com.notemind.framework.audit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 审计日志的异步执行器。
 *
 * <p>必须显式定义：Spring 默认的 {@code @Async} 执行器是 {@code SimpleAsyncTaskExecutor}，
 * 它不给线程数设上限，请求一多就会无限开线程。
 *
 * <p>拒绝策略不能用 {@code CallerRunsPolicy} —— 队列满时它会退回调用者线程执行，
 * 若此刻数据库不可用，异常就会直接打进登录流程，正好违反「审计不得影响登录」。
 * 这里改为记 ERROR 后丢弃：丢一条记录是遗憾，把用户挡在门外是故障。
 */
@Slf4j
@Configuration
@EnableAsync
public class AuditAsyncConfig {

    static final String EXECUTOR_NAME = "auditExecutor";

    @Bean(EXECUTOR_NAME)
    public ThreadPoolTaskExecutor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler((task, pool) -> log.error(
                "审计队列已满（活跃线程 {}，排队 {}），本条记录被丢弃",
                pool.getActiveCount(), pool.getQueue().size()));
        executor.initialize();
        return executor;
    }
}
