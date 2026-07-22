package com.notemind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.entity.Message;

@Mapper

public interface MessageMapper extends BaseMapper<Message> {
}
