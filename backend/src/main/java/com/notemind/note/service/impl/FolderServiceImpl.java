package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.note.dto.SaveFolderRequest;
import com.notemind.note.entity.Note;
import com.notemind.note.entity.NoteFolder;
import com.notemind.note.entity.NoteNotebook;
import com.notemind.note.mapper.NoteFolderMapper;
import com.notemind.note.mapper.NoteMapper;
import com.notemind.note.mapper.NoteNotebookMapper;
import com.notemind.note.service.FolderService;
import com.notemind.note.vo.FolderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FolderServiceImpl implements FolderService {

    private final NoteFolderMapper folderMapper;
    private final NoteNotebookMapper notebookMapper;
    private final NoteMapper noteMapper;

    @Override
    public List<FolderVO> list(Long userId, Long notebookId) {
        List<NoteFolder> folders = folderMapper.selectList(
                new LambdaQueryWrapper<NoteFolder>()
                        .eq(NoteFolder::getUserId, userId)
                        .eq(NoteFolder::getNotebookId, notebookId)
                        .orderByAsc(NoteFolder::getSortOrder)
                        .orderByAsc(NoteFolder::getId));
        return folders.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FolderVO create(Long userId, SaveFolderRequest request) {
        requireNotebook(userId, request.getNotebookId());
        // 表结构支持多级，但当前 UI 只有一级；先显式拒绝，避免出现 UI 无法展示的层级
        if (request.getParentId() != null) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "暂不支持多级文件夹");
        }
        NoteFolder folder = new NoteFolder();
        folder.setUserId(userId);
        folder.setNotebookId(request.getNotebookId());
        folder.setName(request.getName());
        folder.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        folderMapper.insert(folder);
        return toVO(folder);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FolderVO update(Long userId, Long id, SaveFolderRequest request) {
        NoteFolder folder = requireOwned(userId, id);
        folder.setName(request.getName());
        if (request.getSortOrder() != null) {
            folder.setSortOrder(request.getSortOrder());
        }
        // strictUpdateFill 只填充 null 字段，从数据库读出的 updatedAt 非 null，不会自动刷新
        folder.setUpdatedAt(OffsetDateTime.now());
        folderMapper.updateById(folder);
        return toVO(folder);
    }

    /**
     * 删除文件夹不删笔记：其下笔记回到笔记本根目录。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        requireOwned(userId, id);
        noteMapper.update(null, new LambdaUpdateWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(Note::getFolderId, id)
                .set(Note::getFolderId, null)
                .set(Note::getUpdatedAt, OffsetDateTime.now()));
        folderMapper.deleteById(id);
    }

    private NoteFolder requireOwned(Long userId, Long id) {
        NoteFolder folder = folderMapper.selectOne(
                new LambdaQueryWrapper<NoteFolder>()
                        .eq(NoteFolder::getId, id)
                        .eq(NoteFolder::getUserId, userId));
        if (folder == null) {
            throw new BusinessException(ErrorCode.FOLDER_NOT_FOUND);
        }
        return folder;
    }

    private void requireNotebook(Long userId, Long notebookId) {
        Long count = notebookMapper.selectCount(
                new LambdaQueryWrapper<NoteNotebook>()
                        .eq(NoteNotebook::getId, notebookId)
                        .eq(NoteNotebook::getUserId, userId));
        if (count == 0) {
            throw new BusinessException(ErrorCode.NOTEBOOK_NOT_FOUND);
        }
    }

    private FolderVO toVO(NoteFolder folder) {
        FolderVO vo = new FolderVO();
        vo.setId(folder.getId());
        vo.setUserId(folder.getUserId());
        vo.setNotebookId(folder.getNotebookId());
        vo.setParentId(folder.getParentId());
        vo.setName(folder.getName());
        vo.setSortOrder(folder.getSortOrder());
        vo.setCreatedAt(folder.getCreatedAt());
        vo.setUpdatedAt(folder.getUpdatedAt());
        return vo;
    }
}
