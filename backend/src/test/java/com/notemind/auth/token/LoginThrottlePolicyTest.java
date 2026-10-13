package com.notemind.auth.token;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 登录递进延迟的计算规则。
 *
 * <p>纯计算，不依赖 Redis，所以能直接测。默认参数：阈值 5、基数 500ms、上限 2s。
 */
class LoginThrottlePolicyTest {

    private static final int THRESHOLD = 5;
    private static final long BASE = 500L;
    private static final long MAX = 2000L;

    private static long delay(int failures) {
        return LoginThrottlePolicy.delayMillis(failures, THRESHOLD, BASE, MAX);
    }

    @Test
    @DisplayName("未达阈值不延迟 —— 正常用户输错几次不该被拖慢")
    void belowThresholdNoDelay() {
        for (int failures = 0; failures < THRESHOLD; failures++) {
            assertThat(delay(failures)).as("失败 %d 次", failures).isZero();
        }
    }

    @ParameterizedTest
    @DisplayName("达阈值后逐次翻倍，并在上限封顶")
    @CsvSource({
            "5, 500",
            "6, 1000",
            "7, 2000",
            "8, 2000",
            "50, 2000",
    })
    void doublesAndCaps(int failures, long expected) {
        assertThat(delay(failures)).isEqualTo(expected);
    }

    @Test
    @DisplayName("失败次数极大时仍返回上限，不溢出成负数")
    void hugeFailureCountStaysCapped() {
        assertThat(delay(Integer.MAX_VALUE)).isEqualTo(MAX);
    }

    @Test
    @DisplayName("失败次数越大延迟不会反而变小，且始终落在 [0, 上限]")
    void monotonicAndBounded() {
        long previous = -1;
        for (int failures = 0; failures <= 200; failures++) {
            long current = delay(failures);
            assertThat(current).as("失败 %d 次", failures).isGreaterThanOrEqualTo(previous);
            assertThat(current).as("失败 %d 次", failures).isBetween(0L, MAX);
            previous = current;
        }
    }

    @Test
    @DisplayName("上限低于基数时，返回值不超过上限")
    void maxBelowBase() {
        assertThat(LoginThrottlePolicy.delayMillis(10, 1, 5000L, 1000L)).isEqualTo(1000L);
    }
}
