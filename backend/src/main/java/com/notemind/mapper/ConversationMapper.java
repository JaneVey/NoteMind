package com.notemind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.entity.Conversation;

@Mapper

public interface ConversationMapper extends BaseMapper<Conversation> {
}
