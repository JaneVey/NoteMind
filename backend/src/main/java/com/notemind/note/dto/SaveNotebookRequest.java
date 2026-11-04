package com.notemind.note.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SaveNotebookRequest {

    @NotBlank(message = "笔记本名称不能为空")
    @Size(max = 255, message = "笔记本名称不能超过 255 个字符")
    private String name;

    @Size(max = 1000, message = "描述不能超过 1000 个字符")
    private String description;

    @Size(max = 50, message = "图标不能超过 50 个字符")
    private String icon;

    private Integer sortOrder;
}
