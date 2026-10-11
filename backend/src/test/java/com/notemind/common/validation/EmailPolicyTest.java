package com.notemind.common.validation;

import com.notemind.auth.dto.RegisterRequest;
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
 * 邮箱规则的测试。
 *
 * <p>改成现在这条规则之前，用的正则是 {@code ^[^\s@]+@[^\s@]+\.[^\s@]+$}，
 * 那个写法除了"有 @ 且 @ 后面有点"什么都不管：单字母后缀、连续的点、
 * 以点开头、域名里有下划线，全都能通过。
 */
class EmailPolicyTest {

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

    /** 邮箱放进注册请求后是否通过校验（只看 email 字段） */
    private static boolean accepts(String email) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("someone");
        request.setPassword("pass1234");
        request.setEmail(email);
        return validator.validate(request).stream()
                .noneMatch(v -> "email".equals(v.getPropertyPath().toString()));
    }

    @ParameterizedTest
    @DisplayName("接受真实存在的邮箱形态")
    @ValueSource(strings = {
            "user@example.com",
            "admin@notemind.local",
            "2129536594@qq.com",
            "janvey70@gmail.com",
            "a@b.co",
            "first.last@sub.example.com",
            "user+tag@example.com",
            "user_name@example.cn",
            "x@example.com.cn",
            "someone@example.photography",
            "a@example.technology",
    })
    void acceptsRealLooking(String email) {
        assertThat(accepts(email)).as("应当接受：%s", email).isTrue();
    }

    @ParameterizedTest
    @DisplayName("拒绝：后缀不是真实存在的（这是用户报的那个例子）")
    @ValueSource(strings = {
            "a@1.com.cpmmssksksksjsjsjsdjsj",
            "a@example.abcdefghij",
            "a@b.c",
            "a@example.123",
    })
    void rejectsUnknownTld(String email) {
        assertThat(accepts(email)).as("应当拒绝：%s", email).isFalse();
    }

    @ParameterizedTest
    @DisplayName("拒绝：本地部分的常见写法错误")
    @ValueSource(strings = {
            "a..b@x.com",
            ".a@x.com",
            "a.@x.com",
            "a b@x.com",
            "a@@x.com",
            "@x.com",
            "a@",
            "a@x",
    })
    void rejectsBadLocalPart(String email) {
        assertThat(accepts(email)).as("应当拒绝：%s", email).isFalse();
    }

    @ParameterizedTest
    @DisplayName("拒绝：域名部分的常见写法错误")
    @ValueSource(strings = {
            "a@-x.com",
            "a@x-.com",
            "a@x.com.",
            "a@x_y.com",
            "a@x..com",
            "a@.x.com",
    })
    void rejectsBadDomain(String email) {
        assertThat(accepts(email)).as("应当拒绝：%s", email).isFalse();
    }

    @Test
    @DisplayName("本地部分不能超过 64 字符")
    void localPartLengthLimit() {
        assertThat(accepts("a".repeat(64) + "@x.com")).isTrue();
        assertThat(accepts("a".repeat(65) + "@x.com")).isFalse();
    }

    @Test
    @DisplayName("总长 254 接受，255 拒绝")
    void totalLengthLimit() {
        String local = "a".repeat(64);

        // 63+1+63+1+57+4 = 189，64+1+189 = 254
        String domain254 = "b".repeat(63) + "." + "b".repeat(63) + "." + "b".repeat(57) + ".com";
        assertThat(domain254.length()).isEqualTo(189);
        assertThat((local + "@" + domain254).length()).isEqualTo(254);
        assertThat(accepts(local + "@" + domain254)).isTrue();

        // 最后一段多一个字符，总长到 255
        String domain255 = "b".repeat(63) + "." + "b".repeat(63) + "." + "b".repeat(58) + ".com";
        assertThat(domain255.length()).isEqualTo(190);
        assertThat(accepts(local + "@" + domain255)).isFalse();
    }
}
