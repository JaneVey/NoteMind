package com.notemind.note.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.note.entity.NoteFolder;

@Mapper

public interface NoteFolderMapper extends BaseMapper<NoteFolder> {
}
