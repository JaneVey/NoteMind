package com.notemind.note.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateNoteRequest {

    @NotNull(message = "笔记本不能为空")
    private Long notebookId;

    private Long folderId;

    @Size(max = 500, message = "标题不能超过 500 个字符")
    private String title;

    @Size(max = 1_000_000, message = "正文不能超过 100 万个字符")
    private String contentMd;
}
