package com.notemind.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("note")
public class Note {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long notebookId;

    private Long folderId;

    private String title;

    private String contentMd;

    private Integer wordCount;

    @TableField("is_pinned")
    private Boolean pinned;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private OffsetDateTime updatedAt;

    @TableLogic
    @TableField("is_deleted")
    private Boolean deleted;
}
