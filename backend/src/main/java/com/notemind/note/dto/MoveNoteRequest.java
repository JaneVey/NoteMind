package com.notemind.note.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * {@code folderId} 为 null 表示移到笔记本根目录。
 */
@Data
public class MoveNoteRequest {

    @NotNull(message = "目标笔记本不能为空")
    private Long notebookId;

    private Long folderId;
}
