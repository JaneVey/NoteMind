package com.notemind.note.service;

import com.notemind.note.dto.MoveFolderRequest;
import com.notemind.note.dto.SaveFolderRequest;
import com.notemind.note.vo.FolderVO;

import java.util.List;

public interface FolderService {

    List<FolderVO> list(Long userId, Long notebookId);

    FolderVO create(Long userId, SaveFolderRequest request);

    FolderVO update(Long userId, Long id, SaveFolderRequest request);

    FolderVO move(Long userId, Long id, MoveFolderRequest request);

    /**
     * @param force false 时非空文件夹会被拒绝；true 时连同子文件夹与其中的笔记一并删除
     */
    void delete(Long userId, Long id, boolean force);
}
