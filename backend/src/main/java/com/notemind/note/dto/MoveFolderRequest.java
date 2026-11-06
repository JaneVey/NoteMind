package com.notemind.note.dto;

import lombok.Data;

/**
 * 移动文件夹；{@code parentId} 为 null 表示移到笔记本根层。
 */
@Data
public class MoveFolderRequest {

    private Long parentId;
}
