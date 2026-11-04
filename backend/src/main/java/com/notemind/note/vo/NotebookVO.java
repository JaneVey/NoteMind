package com.notemind.note.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class NotebookVO {

    private Long id;

    private Long userId;

    private String name;

    private String description;

    private String icon;

    private Integer sortOrder;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
