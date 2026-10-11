package com.notemind.common.validation;

/**
 * 邮箱规则。
 *
 * <p>只做"形状检查"，用来拦住打字错误和随手输入的乱码。
 * 真正能确认邮箱可用的是发验证邮件（{@code sys_user.email_verified} 就是为此准备的），
 * 正则做不到这件事。
 *
 * <p>约束来自 RFC 5321 / 5322 的实际可执行部分：
 * <ul>
 *   <li>本地部分 1-64 字符，不能以点开头/结尾，也不能有连续的点</li>
 *   <li>域名每段 1-63 字符，只能用字母数字和连字符，且不能以连字符开头或结尾</li>
 *   <li>总长不超过 254 字符</li>
 * </ul>
 *
 * <p>另外限定后缀必须是 2-6 个字母，或在一个常见长后缀白名单里。
 * 这一条不是 RFC 的要求，是为了拦住 {@code a@1.com.cpmmssksksksjsjsjsdjsj}
 * 这类"形状合法但域名根本不存在"的输入。白名单可以按需加。
 */
public final class EmailPolicy {

    /** 总长上限 */
    public static final int MAX_LENGTH = 254;

    /** 常见的长后缀（超过 6 个字母的真实 TLD）。需要时往这里加 */
    private static final String LONG_TLDS =
            "photography|technology|solutions|consulting|foundation|management|engineering"
            + "|construction|community|education|healthcare|university|productions|enterprises"
            + "|marketing|accounting|directory|equipment|furniture|graphics|holdings|lighting"
            + "|mortgage|memorial|training|ventures|vacation|properties|industries|assistance"
            + "|consultant|production|contracting|entertainment|insurance|financial|apartments"
            + "|international";

    /** 本地部分：点用于分隔，所以不能出现连续的点、也不能以点开头或结尾 */
    private static final String LOCAL =
            "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*";

    /** 域名的一段：不能以连字符开头或结尾 */
    private static final String LABEL = "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?";

    private static final String TLD = "(?:[A-Za-z]{2,6}|" + LONG_TLDS + ")";

    /**
     * 开头的 lookahead 限制本地部分不超过 64 字符 ——
     * 用正则写"@ 之前最多 64 个字符"比事后拆字符串判断更省事。
     */
    public static final String REGEX = "^(?=[^@]{1,64}@)" + LOCAL + "@(?:" + LABEL + "\\.)+" + TLD + "$";

    public static final String MESSAGE = "邮箱格式不正确，请填写真实可用的邮箱";

    private EmailPolicy() {
    }
}
