package com.notemind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.entity.Notebook;

@Mapper

public interface NotebookMapper extends BaseMapper<Notebook> {
}
