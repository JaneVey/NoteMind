package com.notemind.auth.token;

/**
 * 登录失败的处置策略：达到多少次开始延迟、延迟多久。
 *
 * <p>纯计算，不碰 Redis 也不碰时钟，便于直接单测。
 */
public final class LoginThrottlePolicy {

    private LoginThrottlePolicy() {
    }

    /**
     * 递进延迟。达到阈值后每多失败一次翻倍，并封顶。
     *
     * <p>阈值 5、基数 500ms、上限 2s 时：5 次 → 500ms，6 次 → 1s，7 次及以上 → 2s。
     * 未达阈值返回 0。
     *
     * <p>这里刻意不做账号锁定：锁定会被攻击者用来锁死别人的账号（拒绝服务），
     * 而递进延迟只惩罚攻击者，正常用户仍能登录成功。
     */
    public static long delayMillis(int failures, int threshold, long baseDelayMillis, long maxDelayMillis) {
        if (failures < threshold) {
            return 0;
        }
        long delay = baseDelayMillis;
        for (int i = threshold; i < failures && delay < maxDelayMillis; i++) {
            delay *= 2;
        }
        return Math.min(delay, maxDelayMillis);
    }
}
