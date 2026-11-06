package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.notemind.common.exception.BusinessException;
import com.notemind.note.dto.UpdateNotebookRequest;
import com.notemind.note.entity.Note;
import com.notemind.note.entity.NoteFolder;
import com.notemind.note.entity.NoteLink;
import com.notemind.note.entity.NoteNotebook;
import com.notemind.note.mapper.NoteFolderMapper;
import com.notemind.note.mapper.NoteLinkMapper;
import com.notemind.note.mapper.NoteMapper;
import com.notemind.note.mapper.NoteNotebookMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotebookServiceImplTest {

    private static final Long USER_ID = 1L;

    private NoteNotebookMapper notebookMapper;
    private NoteFolderMapper folderMapper;
    private NoteMapper noteMapper;
    private NoteLinkMapper noteLinkMapper;
    private NotebookServiceImpl service;

    /** LambdaUpdateWrapper / LambdaQueryWrapper 解析列名依赖实体元数据，纯单测里需要手动注册。 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, NoteNotebook.class);
        TableInfoHelper.initTableInfo(assistant, Note.class);
        TableInfoHelper.initTableInfo(assistant, NoteFolder.class);
        TableInfoHelper.initTableInfo(assistant, NoteLink.class);
    }

    @BeforeEach
    void setUp() {
        notebookMapper = mock(NoteNotebookMapper.class);
        folderMapper = mock(NoteFolderMapper.class);
        noteMapper = mock(NoteMapper.class);
        noteLinkMapper = mock(NoteLinkMapper.class);
        service = new NotebookServiceImpl(notebookMapper, folderMapper, noteMapper, noteLinkMapper);
    }

    @Test
    @DisplayName("笔记本下还有笔记时拒绝删除")
    void deleteRejectsNotEmptyNotebook() {
        when(notebookMapper.selectOne(any())).thenReturn(existingNotebook());
        when(noteMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(USER_ID, 5L, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3004);
        verify(notebookMapper, never()).deleteById(5L);
    }

    @Test
    @DisplayName("强制删除非空笔记本会一并清理笔记、文件夹与链接")
    void forceDeleteRemovesContent() {
        when(notebookMapper.selectOne(any())).thenReturn(existingNotebook());
        when(noteMapper.selectCount(any())).thenReturn(2L);
        when(folderMapper.selectCount(any())).thenReturn(1L);
        Note note = new Note();
        note.setId(10L);
        when(noteMapper.selectList(any())).thenReturn(List.of(note));

        service.delete(USER_ID, 5L, true);

        verify(noteLinkMapper).delete(any());
        verify(noteMapper).delete(any());
        verify(folderMapper).delete(any());
        verify(notebookMapper).deleteById(5L);
    }

    @Test
    @DisplayName("空笔记本可以被删除")
    void deleteEmptyNotebook() {
        when(notebookMapper.selectOne(any())).thenReturn(existingNotebook());
        when(noteMapper.selectCount(any())).thenReturn(0L);
        when(folderMapper.selectCount(any())).thenReturn(0L);

        service.delete(USER_ID, 5L, false);

        verify(notebookMapper).deleteById(5L);
    }

    @Test
    @DisplayName("操作别人的笔记本按不存在处理")
    void updateOtherUsersNotebookIsNotFound() {
        when(notebookMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.update(USER_ID, 5L, new UpdateNotebookRequest()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3002);
    }

    @Test
    @DisplayName("只改描述时不触碰其他字段")
    void updateDescriptionOnly() {
        when(notebookMapper.selectOne(any())).thenReturn(existingNotebook());
        when(notebookMapper.selectById(any())).thenReturn(existingNotebook());
        UpdateNotebookRequest request = new UpdateNotebookRequest();
        request.setDescription("新描述");

        service.update(USER_ID, 5L, request);

        verify(notebookMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("名称改成空白会被拒绝")
    void updateRejectsBlankName() {
        when(notebookMapper.selectOne(any())).thenReturn(existingNotebook());
        UpdateNotebookRequest request = new UpdateNotebookRequest();
        request.setName("   ");

        assertThatThrownBy(() -> service.update(USER_ID, 5L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(1001);
    }

    private NoteNotebook existingNotebook() {
        NoteNotebook notebook = new NoteNotebook();
        notebook.setId(5L);
        notebook.setUserId(USER_ID);
        notebook.setName("我的笔记");
        notebook.setSortOrder(0);
        return notebook;
    }
}
