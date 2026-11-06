package com.notemind.note.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 局部更新：字段为 null 表示不修改。描述传空串表示清空。
 */
@Data
public class UpdateNotebookRequest {

    @Size(max = 255, message = "笔记本名称不能超过 255 个字符")
    private String name;

    @Size(max = 1000, message = "描述不能超过 1000 个字符")
    private String description;

    @Size(max = 500, message = "图标不能超过 500 个字符")
    private String icon;

    private Integer sortOrder;
}
