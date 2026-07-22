package com.notemind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.entity.Note;

@Mapper

public interface NoteMapper extends BaseMapper<Note> {
}
