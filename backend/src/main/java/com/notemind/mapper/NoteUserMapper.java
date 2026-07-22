package com.notemind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.notemind.entity.NoteUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface NoteUserMapper extends BaseMapper<NoteUser> {
}
