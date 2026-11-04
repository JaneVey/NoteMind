package com.notemind.note.vo;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class NoteVO {

    private Long id;

    private Long userId;

    private Long notebookId;

    private Long folderId;

    private String title;

    private String contentMd;

    private Integer wordCount;

    private Boolean pinned;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
