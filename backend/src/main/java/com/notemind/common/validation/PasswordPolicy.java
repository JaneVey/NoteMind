package com.notemind.common.validation;

/**
 * 密码规则。注册与修改密码共用这一份，两处不能各写一套。
 *
 * <p>规则：6-64 位 ASCII 可见字符（不含空格）。
 *
 * <p>限定 ASCII 有两个原因。一是 BCrypt 的上限是 72 <b>字节</b>而不是字符数，
 * 中文一个字 3 字节，按字符数限制会在 25 个中文时就撞上库抛出的异常；
 * 只用 ASCII 之后字符数就等于字节数，上限 64 不可能碰到 72。
 * 二是中文输入法容易打出全角字符，用户下次复现不出来。
 *
 * <p>下限 6 位是产品选择（国内常见做法）。NIST SP 800-63B 建议至少 8 位，
 * 如果以后要过安全评审，改 {@link #MIN_LENGTH} 一个常量即可。
 */
public final class PasswordPolicy {

    /** 长度下限 */
    public static final int MIN_LENGTH = 6;

    /** 长度上限。必须远小于 BCrypt 的 72 字节上限，见类注释 */
    public static final int MAX_LENGTH = 64;

    /** ASCII 可见字符（0x21-0x7E），排除空格 */
    public static final String ALLOWED_REGEX = "^[\\x21-\\x7E]+$";

    public static final String LENGTH_MESSAGE = "密码长度需为 6-64 位";

    public static final String CHARSET_MESSAGE =
            "密码只能使用英文字母、数字和常见符号，不能含空格、中文或全角字符";

    private PasswordPolicy() {
    }
}
