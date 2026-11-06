package com.notemind.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notemind.note.entity.NoteLink;
import com.notemind.note.vo.NoteLinkVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface NoteLinkMapper extends BaseMapper<NoteLink> {

    /** 链接到该笔记的其他笔记 */
    List<NoteLinkVO> selectBacklinks(@Param("userId") Long userId, @Param("noteId") Long noteId);

    /** 该笔记链接出去的笔记 */
    List<NoteLinkVO> selectOutgoing(@Param("userId") Long userId, @Param("noteId") Long noteId);
}
