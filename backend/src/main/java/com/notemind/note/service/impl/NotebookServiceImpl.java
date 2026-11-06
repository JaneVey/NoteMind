package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.note.dto.SaveNotebookRequest;
import com.notemind.note.dto.UpdateNotebookRequest;
import com.notemind.note.entity.Note;
import com.notemind.note.entity.NoteFolder;
import com.notemind.note.entity.NoteLink;
import com.notemind.note.entity.NoteNotebook;
import com.notemind.note.mapper.NoteFolderMapper;
import com.notemind.note.mapper.NoteLinkMapper;
import com.notemind.note.mapper.NoteMapper;
import com.notemind.note.mapper.NoteNotebookMapper;
import com.notemind.note.service.NotebookService;
import com.notemind.note.vo.NotebookVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotebookServiceImpl implements NotebookService {

    private final NoteNotebookMapper notebookMapper;
    private final NoteFolderMapper folderMapper;
    private final NoteMapper noteMapper;
    private final NoteLinkMapper noteLinkMapper;

    @Override
    public List<NotebookVO> list(Long userId) {
        List<NoteNotebook> notebooks = notebookMapper.selectList(
                new LambdaQueryWrapper<NoteNotebook>()
                        .eq(NoteNotebook::getUserId, userId)
                        .orderByAsc(NoteNotebook::getSortOrder)
                        .orderByAsc(NoteNotebook::getId));
        return notebooks.stream().map(this::toVO).toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotebookVO create(Long userId, SaveNotebookRequest request) {
        NoteNotebook notebook = new NoteNotebook();
        notebook.setUserId(userId);
        notebook.setName(request.getName());
        notebook.setDescription(request.getDescription());
        notebook.setIcon(request.getIcon());
        notebook.setSortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder());
        notebookMapper.insert(notebook);
        return toVO(notebook);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotebookVO update(Long userId, Long id, UpdateNotebookRequest request) {
        requireOwned(userId, id);

        LambdaUpdateWrapper<NoteNotebook> wrapper = new LambdaUpdateWrapper<NoteNotebook>()
                .eq(NoteNotebook::getId, id)
                .eq(NoteNotebook::getUserId, userId);
        boolean changed = false;

        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new BusinessException(ErrorCode.PARAM_INVALID, "笔记本名称不能为空");
            }
            wrapper.set(NoteNotebook::getName, request.getName().trim());
            changed = true;
        }
        if (request.getDescription() != null) {
            // 空串是"清空"的显式表达；PATCH 里 null 代表不修改，无法直接传 null
            wrapper.set(NoteNotebook::getDescription,
                    request.getDescription().isBlank() ? null : request.getDescription());
            changed = true;
        }
        if (request.getIcon() != null) {
            wrapper.set(NoteNotebook::getIcon, request.getIcon());
            changed = true;
        }
        if (request.getSortOrder() != null) {
            wrapper.set(NoteNotebook::getSortOrder, request.getSortOrder());
            changed = true;
        }
        if (changed) {
            wrapper.set(NoteNotebook::getUpdatedAt, OffsetDateTime.now());
            notebookMapper.update(null, wrapper);
        }
        NoteNotebook updated = notebookMapper.selectById(id);
        if (updated == null) {
            throw new BusinessException(ErrorCode.NOTEBOOK_NOT_FOUND);
        }
        return toVO(updated);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id, boolean force) {
        requireOwned(userId, id);
        Long noteCount = noteMapper.selectCount(new LambdaQueryWrapper<Note>()
                .eq(Note::getUserId, userId)
                .eq(Note::getNotebookId, id));
        Long folderCount = folderMapper.selectCount(new LambdaQueryWrapper<NoteFolder>()
                .eq(NoteFolder::getUserId, userId)
                .eq(NoteFolder::getNotebookId, id));
        if (!force && (noteCount > 0 || folderCount > 0)) {
            throw new BusinessException(ErrorCode.NOTEBOOK_NOT_EMPTY,
                    "笔记本下还有 " + noteCount + " 篇笔记、" + folderCount + " 个文件夹");
        }

        if (force) {
            List<Long> noteIds = noteMapper.selectList(new LambdaQueryWrapper<Note>()
                            .select(Note::getId)
                            .eq(Note::getUserId, userId)
                            .eq(Note::getNotebookId, id))
                    .stream().map(Note::getId).toList();
            if (!noteIds.isEmpty()) {
                // note_link 是派生数据，硬删；两端任一落在被删笔记本内都要清掉
                noteLinkMapper.delete(new LambdaQueryWrapper<NoteLink>()
                        .eq(NoteLink::getUserId, userId)
                        .and(wrapper -> wrapper
                                .in(NoteLink::getSourceNoteId, noteIds)
                                .or()
                                .in(NoteLink::getTargetNoteId, noteIds)));
            }
            noteMapper.delete(new LambdaQueryWrapper<Note>()
                    .eq(Note::getUserId, userId)
                    .eq(Note::getNotebookId, id));
            folderMapper.delete(new LambdaQueryWrapper<NoteFolder>()
                    .eq(NoteFolder::getUserId, userId)
                    .eq(NoteFolder::getNotebookId, id));
        }
        notebookMapper.deleteById(id);
    }

    private NoteNotebook requireOwned(Long userId, Long id) {
        NoteNotebook notebook = notebookMapper.selectOne(
                new LambdaQueryWrapper<NoteNotebook>()
                        .eq(NoteNotebook::getId, id)
                        .eq(NoteNotebook::getUserId, userId));
        if (notebook == null) {
            throw new BusinessException(ErrorCode.NOTEBOOK_NOT_FOUND);
        }
        return notebook;
    }

    private NotebookVO toVO(NoteNotebook notebook) {
        NotebookVO vo = new NotebookVO();
        vo.setId(notebook.getId());
        vo.setUserId(notebook.getUserId());
        vo.setName(notebook.getName());
        vo.setDescription(notebook.getDescription());
        vo.setIcon(notebook.getIcon());
        vo.setSortOrder(notebook.getSortOrder());
        vo.setCreatedAt(notebook.getCreatedAt());
        vo.setUpdatedAt(notebook.getUpdatedAt());
        return vo;
    }
}
