package com.notemind.framework.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 审计写入的重试，以及「绝不向外抛」这条约束。
 *
 * <p>直接用 mock 的 mapper 构造 recorder，不经过 Spring 的 @Async 代理 ——
 * 走代理调用会变成异步，断言就没法做了。
 */
class OperationLogRecorderTest {

    private final SysOperationLogMapper mapper = mock(SysOperationLogMapper.class);

    private final OperationLogRecorder recorder = new OperationLogRecorder(mapper);

    private static SysOperationLog entry() {
        SysOperationLog entry = new SysOperationLog();
        entry.setModule("auth");
        entry.setOperation("LOGIN");
        return entry;
    }

    @Test
    @DisplayName("一次写入成功就只写一次，不做多余重试")
    void writesOnceOnSuccess() {
        when(mapper.insert(any(SysOperationLog.class))).thenReturn(1);

        recorder.record(entry());

        verify(mapper, times(1)).insert(any(SysOperationLog.class));
    }

    @Test
    @DisplayName("前两次失败会重试，第三次成功")
    void retriesThenSucceeds() {
        when(mapper.insert(any(SysOperationLog.class)))
                .thenThrow(new RuntimeException("database down"))
                .thenThrow(new RuntimeException("database down"))
                .thenReturn(1);

        recorder.record(entry());

        verify(mapper, times(3)).insert(any(SysOperationLog.class));
    }

    @Test
    @DisplayName("重试耗尽也不向外抛异常 —— 审计失败不能连累登录")
    void neverPropagatesFailure() {
        when(mapper.insert(any(SysOperationLog.class)))
                .thenThrow(new RuntimeException("database down"));

        assertThatCode(() -> recorder.record(entry())).doesNotThrowAnyException();
        verify(mapper, times(3)).insert(any(SysOperationLog.class));
    }
}
