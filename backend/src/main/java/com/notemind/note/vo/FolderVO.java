package com.notemind.note.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class FolderVO {

    private Long id;

    private Long userId;

    private Long notebookId;

    private Long parentId;

    private String name;

    private Integer sortOrder;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
