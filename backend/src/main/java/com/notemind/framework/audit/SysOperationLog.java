package com.notemind.framework.audit;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 操作日志（数据库表 {@code sys_operation_log}）。
 *
 * <p>本表只追加、不修改，所以没有 {@code updatedAt} —— 库里也确实没有这一列。
 * 目前只有认证模块写入，其余列（{@code httpMethod} / {@code requestUri} / {@code durationMs}）
 * 留待通用 HTTP 审计接入时再填，现在填只会是废数据。
 *
 * <p>{@code requestParam} 是请求参数快照，写入前必须脱敏，禁止出现密码与 API Key。
 */
@Data
@TableName("sys_operation_log")
public class SysOperationLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 账号不存在时为 null。失败记录同样要留 —— 被攻击时才看得见的正是这些 */
    private Long userId;

    /** 模块：auth / user / note / knowledge / ai */
    private String module;

    /** 操作名：LOGIN / REGISTER … */
    private String operation;

    private String httpMethod;

    private String requestUri;

    private String requestParam;

    private Boolean success;

    /** 失败时的业务错误码，见 {@code ErrorCode} */
    private Integer errorCode;

    private String errorMessage;

    private String clientIp;

    private String userAgent;

    private Integer durationMs;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
