package com.notemind.note.vo;

import lombok.Data;

/**
 * 链接或反链指向的笔记，右侧面板只需要 id 与标题。
 */
@Data
public class NoteLinkVO {

    private Long id;

    private String title;
}
