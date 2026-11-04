package com.notemind.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notemind.note.entity.Note;
import com.notemind.note.vo.NoteListItemVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteMapper extends BaseMapper<Note> {

    /**
     * 列表查询，正文只取前 200 个字符作为摘要。
     *
     * <p>自定义 SQL 不会自动带逻辑删除条件，{@code is_deleted = FALSE} 必须手写。
     *
     * @param folderId 为 null 时不按文件夹过滤
     * @param keyword  为 null 或空串时不按关键词过滤
     */
    List<NoteListItemVO> selectListItems(@Param("userId") Long userId,
                                         @Param("notebookId") Long notebookId,
                                         @Param("folderId") Long folderId,
                                         @Param("keyword") String keyword);
}
