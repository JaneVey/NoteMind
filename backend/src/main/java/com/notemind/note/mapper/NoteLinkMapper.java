package com.notemind.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.note.entity.NoteLink;

@Mapper

public interface NoteLinkMapper extends BaseMapper<NoteLink> {
}
