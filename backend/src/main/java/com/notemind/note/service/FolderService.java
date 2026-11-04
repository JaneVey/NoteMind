package com.notemind.note.service;

import com.notemind.note.dto.SaveFolderRequest;
import com.notemind.note.vo.FolderVO;

import java.util.List;

public interface FolderService {

    List<FolderVO> list(Long userId, Long notebookId);

    FolderVO create(Long userId, SaveFolderRequest request);

    FolderVO update(Long userId, Long id, SaveFolderRequest request);

    void delete(Long userId, Long id);
}
