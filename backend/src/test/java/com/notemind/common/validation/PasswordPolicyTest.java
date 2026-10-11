package com.notemind.common.validation;

import com.notemind.auth.dto.RegisterRequest;
import com.notemind.user.dto.UpdatePasswordRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 密码规则的测试。
 *
 * <p>重点是第一个用例：注册与修改密码必须对同一个密码给出相同判断。
 * 这两处原先各写了一套注解（8 位起 vs 6 位起，100 字符上限），
 * 而且都没限制字符集，中文和全角字符能直接通过。
 */
class PasswordPolicyTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    /** 密码放进注册请求后是否通过校验（只看 password 字段，其余字段填合法值） */
    private static boolean registerAccepts(String password) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("someone");
        request.setEmail("someone@example.com");
        request.setPassword(password);
        return validator.validate(request).stream()
                .noneMatch(v -> "password".equals(v.getPropertyPath().toString()));
    }

    /** 密码放进改密码请求后是否通过校验 */
    private static boolean changeAccepts(String password) {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setOldPassword("whatever");
        request.setNewPassword(password);
        return validator.validate(request).stream()
                .noneMatch(v -> "newPassword".equals(v.getPropertyPath().toString()));
    }

    @Test
    @DisplayName("注册与修改密码对同一个密码的判断必须一致")
    void bothEndpointsAgree() {
        String[] samples = {
                "", "a", "a".repeat(7), "a".repeat(8), "a".repeat(64), "a".repeat(65), "a".repeat(100),
                "pass1234", "中文密码中文密码", "１２３４５６７８", "🔒🔒🔒🔒🔒🔒🔒🔒",
                "pass word 123", "abc中文1234", "中".repeat(25), "P@ssw0rd!",
        };
        for (String sample : samples) {
            assertThat(registerAccepts(sample))
                    .as("密码 [%s] 在注册与改密码两处的判断不一致", sample)
                    .isEqualTo(changeAccepts(sample));
        }
    }

    @ParameterizedTest
    @DisplayName("拒绝非 ASCII 字符：中文、全角数字、emoji、空格")
    @ValueSource(strings = {
            "中文密码中文密码",
            "１２３４５６７８",
            "🔒🔒🔒🔒🔒🔒🔒🔒",
            "pass word 123",
            "abc中文1234",
            "中中中中中中中中",
    })
    void rejectsNonAscii(String password) {
        assertThat(registerAccepts(password)).as("应当拒绝：%s", password).isFalse();
    }

    @ParameterizedTest
    @DisplayName("接受 8-64 位的 ASCII 可见字符")
    @ValueSource(strings = {
            "pass1234",
            "aaaaaaaa",
            "P@ssw0rd!",
            "~!@#$%^&*()_+",
            "0123456789",
            // 26 + 26 + 10 + 1 = 63 位，刚好在上限内
            "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789!",
    })
    void acceptsAscii(String password) {
        assertThat(registerAccepts(password)).as("应当接受：%s", password).isTrue();
    }

    @Test
    @DisplayName("长度边界：7 位拒绝，8 与 64 位接受，65 位拒绝")
    void lengthBoundary() {
        assertThat(registerAccepts("a".repeat(PasswordPolicy.MIN_LENGTH - 1))).isFalse();
        assertThat(registerAccepts("a".repeat(PasswordPolicy.MIN_LENGTH))).isTrue();
        assertThat(registerAccepts("a".repeat(PasswordPolicy.MAX_LENGTH))).isTrue();
        assertThat(registerAccepts("a".repeat(PasswordPolicy.MAX_LENGTH + 1))).isFalse();
    }

    @Test
    @DisplayName("上限必须低于 BCrypt 的 72 字节，否则库里会抛异常且报英文错误")
    void maxLengthStaysUnderBcryptLimit() {
        assertThat(PasswordPolicy.MAX_LENGTH).isLessThan(72);
    }
}
