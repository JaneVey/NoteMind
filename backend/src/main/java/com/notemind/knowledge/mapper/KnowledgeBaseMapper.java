package com.notemind.knowledge.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.knowledge.entity.KnowledgeBase;

@Mapper

public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {
}
