package com.notemind.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.entity.UserProfile;

@Mapper

public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
