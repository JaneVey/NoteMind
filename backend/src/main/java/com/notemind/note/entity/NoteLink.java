package com.notemind.note.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("note_link")
public class NoteLink {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long sourceNoteId;

    private Long targetNoteId;

    private String linkType;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
