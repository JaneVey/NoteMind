package com.notemind.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveFolderRequest {

    @NotNull(message = "笔记本不能为空")
    private Long notebookId;

    private Long parentId;

    @NotBlank(message = "文件夹名称不能为空")
    @Size(max = 255, message = "文件夹名称不能超过 255 个字符")
    private String name;

    private Integer sortOrder;
}
