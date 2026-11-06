package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.notemind.common.exception.BusinessException;
import com.notemind.note.dto.MoveFolderRequest;
import com.notemind.note.entity.Note;
import com.notemind.note.entity.NoteFolder;
import com.notemind.note.entity.NoteLink;
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

class FolderServiceImplTest {

    private static final Long USER_ID = 1L;

    private NoteFolderMapper folderMapper;
    private NoteMapper noteMapper;
    private NoteLinkMapper noteLinkMapper;
    private FolderServiceImpl service;

    /** Lambda 条件解析依赖实体元数据，纯单测里没有 Spring 容器，需要手动注册。 */
    @BeforeAll
    static void initTableInfo() {
        MapperBuilderAssistant assistant =
                new MapperBuilderAssistant(new MybatisConfiguration(), "test");
        TableInfoHelper.initTableInfo(assistant, Note.class);
        TableInfoHelper.initTableInfo(assistant, NoteFolder.class);
        TableInfoHelper.initTableInfo(assistant, NoteLink.class);
    }

    @BeforeEach
    void setUp() {
        folderMapper = mock(NoteFolderMapper.class);
        noteMapper = mock(NoteMapper.class);
        noteLinkMapper = mock(NoteLinkMapper.class);
        service = new FolderServiceImpl(
                folderMapper, mock(NoteNotebookMapper.class), noteMapper, noteLinkMapper);
    }

    @Test
    @DisplayName("空文件夹直接删除")
    void deleteEmptyFolder() {
        when(folderMapper.selectOne(any())).thenReturn(existingFolder());
        when(folderMapper.selectList(any())).thenReturn(List.of(existingFolder()));
        when(noteMapper.selectCount(any())).thenReturn(0L);

        service.delete(USER_ID, 3L, false);

        verify(folderMapper).deleteById(3L);
    }

    @Test
    @DisplayName("文件夹下有内容时默认拒绝删除")
    void deleteRejectsNotEmptyFolder() {
        when(folderMapper.selectOne(any())).thenReturn(existingFolder());
        when(folderMapper.selectList(any())).thenReturn(List.of(existingFolder()));
        when(noteMapper.selectCount(any())).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(USER_ID, 3L, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3005);
        verify(folderMapper, never()).deleteById(3L);
    }

    @Test
    @DisplayName("强制删除会清理子文件夹、笔记与链接")
    void forceDeleteRemovesSubtree() {
        when(folderMapper.selectOne(any())).thenReturn(existingFolder());
        when(folderMapper.selectList(any())).thenReturn(List.of(existingFolder()));
        when(noteMapper.selectCount(any())).thenReturn(2L);
        Note note = new Note();
        note.setId(10L);
        when(noteMapper.selectList(any())).thenReturn(List.of(note));

        service.delete(USER_ID, 3L, true);

        verify(noteLinkMapper).delete(any());
        verify(noteMapper).delete(any());
        verify(folderMapper).delete(any());
    }

    @Test
    @DisplayName("移动文件夹到根层")
    void moveFolderToRoot() {
        when(folderMapper.selectOne(any())).thenReturn(existingFolder());
        when(folderMapper.selectById(any())).thenReturn(existingFolder());

        service.move(USER_ID, 3L, new MoveFolderRequest());

        verify(folderMapper).update(isNull(), any());
    }

    @Test
    @DisplayName("删除别人的文件夹按不存在处理")
    void deleteOtherUsersFolderIsNotFound() {
        when(folderMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.delete(USER_ID, 3L, false))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3003);
    }

    private NoteFolder existingFolder() {
        NoteFolder folder = new NoteFolder();
        folder.setId(3L);
        folder.setUserId(USER_ID);
        folder.setNotebookId(1L);
        folder.setName("资料");
        return folder;
    }
}
