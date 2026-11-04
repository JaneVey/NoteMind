package com.notemind.note.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 部分更新。字段为 null 表示不修改；移动文件夹走 {@code POST /api/notes/{id}/move}。
 */
@Data
public class UpdateNoteRequest {

    @Size(max = 500, message = "标题不能超过 500 个字符")
    private String title;

    @Size(max = 1_000_000, message = "正文不能超过 100 万个字符")
    private String contentMd;

    private Boolean pinned;
}
