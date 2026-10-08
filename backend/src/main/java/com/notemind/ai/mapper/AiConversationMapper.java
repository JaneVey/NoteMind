package com.notemind.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.ai.entity.AiConversation;

@Mapper

public interface AiConversationMapper extends BaseMapper<AiConversation> {
}
