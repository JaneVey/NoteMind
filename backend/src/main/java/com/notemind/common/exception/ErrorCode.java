package com.notemind.common.exception;

import lombok.Getter;

/**
 * 业务错误码。
 *
 * <p><b>为什么要把「HTTP 状态码」和「业务码」分开</b>
 *
 * <p>项目此前用 {@code BusinessException(400, "用户名已存在")} 与 {@code (400, "旧密码错误")} ——
 * 两个完全不同的失败原因共用同一个 400，前端无法区分，日志里也定位不到模块。
 *
 * <p>现约定：
 * <ul>
 *   <li><b>HTTP 状态码</b>表达协议语义：400 参数/业务拒绝、401 未认证、403 无权限、404 资源不存在、500 服务端错误</li>
 *   <li><b>业务码</b>精确定位原因，按模块分段：1xxx 通用、2xxx 用户与认证、3xxx 笔记、4xxx 知识库、5xxx AI</li>
 * </ul>
 *
 * <p>分段的好处：看日志里的 code 就知道是哪个模块的问题；前端可以针对具体原因做差异化处理，
 * 而不是拿到一个笼统的 400 只能把后端返回的文案弹出来。
 *
 * <p><b>新增错误码的规则</b>：在对应模块段内追加，不要复用别人的码；不要跳号太多。
 */
@Getter
public enum ErrorCode {

    // ---------- 1xxx 通用 ----------
    PARAM_INVALID(1001, 400, "参数校验失败"),
    UNAUTHORIZED(1002, 401, "未登录或登录已过期"),
    FORBIDDEN(1003, 403, "没有访问权限"),
    RESOURCE_NOT_FOUND(1004, 404, "资源不存在"),
    SYSTEM_ERROR(1005, 500, "服务器内部错误"),
    METHOD_NOT_ALLOWED(1006, 405, "请求方法不支持"),

    // ---------- 2xxx 用户与认证 ----------
    USERNAME_EXISTS(2001, 400, "用户名已存在"),
    LOGIN_FAILED(2002, 401, "用户名或密码错误"),
    USER_NOT_FOUND(2003, 400, "用户不存在或已注销"),
    OLD_PASSWORD_WRONG(2004, 400, "旧密码错误"),
    PASSWORD_UNCHANGED(2005, 400, "新密码不能与旧密码相同"),
    OAUTH_ACCOUNT_ONLY(2006, 400, "该账号使用第三方登录，请用 GitHub 登录"),
    LOGIN_TOO_FREQUENT(2007, 429, "登录尝试过于频繁，请稍后再试"),
    EMAIL_EXISTS(2008, 400, "邮箱已被注册"),
    REFRESH_TOKEN_INVALID(2009, 401, "登录状态已失效，请重新登录"),
    OAUTH_FAILED(2010, 400, "第三方登录失败，请重试"),
    OAUTH_ALREADY_BOUND(2011, 400, "该第三方账号已绑定其他用户"),
    ACCOUNT_DISABLED(2012, 403, "账号已被禁用"),

    // ---------- 3xxx 笔记 ----------
    NOTE_NOT_FOUND(3001, 404, "笔记不存在"),
    NOTEBOOK_NOT_FOUND(3002, 404, "笔记本不存在"),
    FOLDER_NOT_FOUND(3003, 404, "文件夹不存在"),
    NOTEBOOK_NOT_EMPTY(3004, 400, "笔记本下还有内容，无法删除"),

    // ---------- 4xxx 知识库 ----------
    KNOWLEDGE_BASE_NOT_FOUND(4001, 404, "知识库不存在"),
    DOCUMENT_NOT_FOUND(4002, 404, "文档不存在"),
    DOCUMENT_PARSE_FAILED(4003, 400, "文档解析失败"),
    DOCUMENT_NEED_OCR(4004, 400, "该文件无文本层，需 OCR（暂不支持）"),
    DOCUMENT_UNSUPPORTED_TYPE(4005, 400, "不支持的文件类型"),

    // ---------- 5xxx AI ----------
    AI_NOT_CONFIGURED(5001, 400, "尚未配置 AI 模型"),
    AI_CALL_FAILED(5002, 500, "模型调用失败");

    /** 业务码，返回给前端的 {@code Result.code} */
    private final int code;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    /** 默认提示文案（可被 BusinessException 覆盖） */
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }
}
