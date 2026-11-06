package com.notemind.note.service;

import com.notemind.note.dto.SaveNotebookRequest;
import com.notemind.note.dto.UpdateNotebookRequest;
import com.notemind.note.vo.NotebookVO;

import java.util.List;

public interface NotebookService {

    List<NotebookVO> list(Long userId);

    NotebookVO create(Long userId, SaveNotebookRequest request);

    /** 局部更新：只处理请求中出现的字段 */
    NotebookVO update(Long userId, Long id, UpdateNotebookRequest request);

    /**
     * @param force false 时非空笔记本会被拒绝；true 时连同笔记、文件夹与链接一并删除
     */
    void delete(Long userId, Long id, boolean force);
}
