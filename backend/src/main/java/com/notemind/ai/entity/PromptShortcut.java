package com.notemind.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("prompt_shortcut")
public class PromptShortcut {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String name;

    private String prompt;

    private String description;

    private Boolean isPreset;

    private Integer sortOrder;

    private Boolean isEnabled;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;
}
