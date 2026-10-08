package com.notemind.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import com.notemind.user.entity.UserProfile;

@Mapper

public interface UserProfileMapper extends BaseMapper<UserProfile> {
}
