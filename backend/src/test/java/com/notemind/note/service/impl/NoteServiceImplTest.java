package com.notemind.note.service.impl;

import com.notemind.common.exception.BusinessException;
import com.notemind.note.dto.CreateNoteRequest;
import com.notemind.note.dto.MoveNoteRequest;
import com.notemind.note.dto.UpdateNoteRequest;
import com.notemind.note.entity.Note;
import com.notemind.note.mapper.NoteFolderMapper;
import com.notemind.note.mapper.NoteMapper;
import com.notemind.note.mapper.NoteNotebookMapper;
import com.notemind.note.vo.NoteVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteServiceImplTest {

    private static final Long USER_ID = 1L;

    private NoteMapper noteMapper;
    private NoteNotebookMapper notebookMapper;
    private NoteFolderMapper folderMapper;
    private NoteServiceImpl service;

    @BeforeEach
    void setUp() {
        noteMapper = mock(NoteMapper.class);
        notebookMapper = mock(NoteNotebookMapper.class);
        folderMapper = mock(NoteFolderMapper.class);
        service = new NoteServiceImpl(noteMapper, notebookMapper, folderMapper);
    }

    @Test
    @DisplayName("查询别人的笔记按不存在处理，不暴露资源是否真实存在")
    void getOtherUsersNoteIsNotFound() {
        when(noteMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.get(USER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3001);
    }

    @Test
    @DisplayName("创建笔记时标题为空用默认标题，字数按汉字与字母数字分段统计")
    void createFillsTitleAndWordCount() {
        when(notebookMapper.selectCount(any())).thenReturn(1L);
        CreateNoteRequest request = new CreateNoteRequest();
        request.setNotebookId(1L);
        request.setTitle("   ");
        request.setContentMd("Hello world 你好");

        NoteVO vo = service.create(USER_ID, request);

        assertThat(vo.getTitle()).isEqualTo("无标题笔记");
        assertThat(vo.getWordCount()).isEqualTo(4);
        assertThat(vo.getPinned()).isFalse();
        verify(noteMapper).insert(any(Note.class));
    }

    @Test
    @DisplayName("创建笔记时文件夹必须属于目标笔记本")
    void createRejectsFolderFromAnotherNotebook() {
        when(notebookMapper.selectCount(any())).thenReturn(1L);
        when(folderMapper.selectCount(any())).thenReturn(0L);
        CreateNoteRequest request = new CreateNoteRequest();
        request.setNotebookId(1L);
        request.setFolderId(2L);

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3003);
    }

    @Test
    @DisplayName("更新正文时重算字数并刷新更新时间")
    void updateRecalculatesWordCount() {
        Note note = existingNote();
        when(noteMapper.selectOne(any())).thenReturn(note);
        UpdateNoteRequest request = new UpdateNoteRequest();
        request.setContentMd("新增 3 个词");

        NoteVO vo = service.update(USER_ID, note.getId(), request);

        assertThat(vo.getWordCount()).isEqualTo(5);
        assertThat(vo.getUpdatedAt()).isNotNull();
        verify(noteMapper).updateById(note);
    }

    @Test
    @DisplayName("移动笔记时目标文件夹必须属于目标笔记本")
    void moveRejectsFolderFromAnotherNotebook() {
        Note note = existingNote();
        when(noteMapper.selectOne(any())).thenReturn(note);
        when(notebookMapper.selectCount(any())).thenReturn(1L);
        when(folderMapper.selectCount(any())).thenReturn(0L);
        MoveNoteRequest request = new MoveNoteRequest();
        request.setNotebookId(2L);
        request.setFolderId(7L);

        assertThatThrownBy(() -> service.move(USER_ID, note.getId(), request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3003);
    }

    @Test
    @DisplayName("删除不存在的笔记返回笔记不存在")
    void deleteMissingNoteIsNotFound() {
        when(noteMapper.delete(any())).thenReturn(0);

        assertThatThrownBy(() -> service.delete(USER_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3001);
    }

    private Note existingNote() {
        Note note = new Note();
        note.setId(10L);
        note.setUserId(USER_ID);
        note.setNotebookId(1L);
        note.setTitle("原标题");
        note.setContentMd("旧正文");
        note.setWordCount(1);
        note.setPinned(false);
        return note;
    }
}
