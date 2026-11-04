package com.notemind.note.vo;

import lombok.Data;

import java.time.OffsetDateTime;

/**
 * 笔记列表项：不含正文，{@code excerpt} 是正文前 200 个字符。
 */
@Data
public class NoteListItemVO {

    private Long id;

    private Long userId;

    private Long notebookId;

    private Long folderId;

    private String title;

    private String excerpt;

    private Integer wordCount;

    private Boolean pinned;

    private OffsetDateTime createdAt;

    private OffsetDateTime updatedAt;
}
