package com.notemind.note.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.notemind.common.exception.BusinessException;
import com.notemind.note.entity.Note;
import com.notemind.note.entity.NoteFolder;
import com.notemind.note.mapper.NoteFolderMapper;
import com.notemind.note.mapper.NoteMapper;
import com.notemind.note.mapper.NoteNotebookMapper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FolderServiceImplTest {

    private static final Long USER_ID = 1L;

    private NoteFolderMapper folderMapper;
    private NoteMapper noteMapper;
    private FolderServiceImpl service;

    /** LambdaUpdateWrapper 解析列名依赖实体元数据，纯单测里没有 Spring 容器，需要手动注册一次。 */
    @BeforeAll
    static void initTableInfo() {
        TableInfoHelper.initTableInfo(
                new MapperBuilderAssistant(new MybatisConfiguration(), "test"), Note.class);
    }

    @BeforeEach
    void setUp() {
        folderMapper = mock(NoteFolderMapper.class);
        noteMapper = mock(NoteMapper.class);
        service = new FolderServiceImpl(folderMapper, mock(NoteNotebookMapper.class), noteMapper);
    }

    @Test
    @DisplayName("删除文件夹时笔记回到根目录，文件夹逻辑删除")
    void deleteMovesNotesToRoot() {
        NoteFolder folder = new NoteFolder();
        folder.setId(3L);
        folder.setUserId(USER_ID);
        folder.setNotebookId(1L);
        folder.setName("资料");
        when(folderMapper.selectOne(any())).thenReturn(folder);

        service.delete(USER_ID, 3L);

        verify(noteMapper).update(isNull(), any());
        verify(folderMapper).deleteById(3L);
    }

    @Test
    @DisplayName("删除别人的文件夹按不存在处理")
    void deleteOtherUsersFolderIsNotFound() {
        when(folderMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.delete(USER_ID, 3L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(3003);
    }
}
