package com.notemind.knowledge.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("knowledge_chunk")
public class KnowledgeChunk {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long knowledgeBaseId;

    private Long noteId;

    private Integer chunkIndex;

    private String chunkContent;

    private Integer chunkTokenCount;

    private String embedding;

    private String sourceInfo;

    @TableField(fill = FieldFill.INSERT)
    private OffsetDateTime createdAt;
}
