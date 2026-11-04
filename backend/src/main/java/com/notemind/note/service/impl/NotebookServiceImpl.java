package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.note.dto.SaveNotebookRequest;
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
    public NotebookVO update(Long userId, Long id, SaveNotebookRequest request) {
        requireOwned(userId, id);
        // updateById 会忽略 null 字段，而 PUT 的语义是「描述/图标可以置空」，必须显式 set
        notebookMapper.update(null, new LambdaUpdateWrapper<NoteNotebook>()
                .eq(NoteNotebook::getId, id)
                .eq(NoteNotebook::getUserId, userId)
                .set(NoteNotebook::getName, request.getName())
                .set(NoteNotebook::getDescription, request.getDescription())
                .set(NoteNotebook::getIcon, request.getIcon())
                .set(NoteNotebook::getSortOrder, request.getSortOrder() == null ? 0 : request.getSortOrder())
                .set(NoteNotebook::getUpdatedAt, OffsetDateTime.now()));
        return toVO(notebookMapper.selectById(id));
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
