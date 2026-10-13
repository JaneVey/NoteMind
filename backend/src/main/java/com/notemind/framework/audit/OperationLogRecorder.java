package com.notemind.framework.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 操作日志写入方：异步 + 有限重试。
 *
 * <p>审计是配角，既不该拖慢登录，更不该让登录失败。写入失败时重试有限次，仍失败则打
 * ERROR —— 丢一条记录要看得到，不能静默。
 *
 * <p>请求相关信息（UA、IP）必须由调用方在请求线程上取好再传进来：异步线程上拿不到
 * {@code HttpServletRequest}，那时请求可能已经被回收。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OperationLogRecorder {

    private static final int MAX_ATTEMPTS = 3;

    private static final long RETRY_BACKOFF_MILLIS = 200;

    private final SysOperationLogMapper mapper;

    /**
     * 异步写入一条操作日志，调用方不会被阻塞，也不会因为它失败而失败。
     *
     * <p>参数名刻意避开 {@code log}：本类有 {@code @Slf4j} 生成的同名字段。
     */
    @Async(AuditAsyncConfig.EXECUTOR_NAME)
    public void record(SysOperationLog logEntry) {
        RuntimeException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                mapper.insert(logEntry);
                return;
            } catch (RuntimeException e) {
                lastError = e;
                sleepBeforeRetry(attempt);
            }
        }
        log.error("操作日志写入失败，重试 {} 次后放弃: module={} operation={} userId={}",
                MAX_ATTEMPTS, logEntry.getModule(), logEntry.getOperation(),
                logEntry.getUserId(), lastError);
    }

    /**
     * 重试之间退避等待。
     *
     * <p>重试理论上可能写出重复行（语句已提交但驱动没拿到确认就会重发），
     * 本表是只追加的审计表，重复一行无害，不值得为它引入去重。
     */
    private void sleepBeforeRetry(int attempt) {
        if (attempt >= MAX_ATTEMPTS) {
            return;
        }
        try {
            Thread.sleep(RETRY_BACKOFF_MILLIS * attempt);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
