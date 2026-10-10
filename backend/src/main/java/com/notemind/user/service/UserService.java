package com.notemind.user.service;

import com.notemind.user.dto.UpdatePasswordRequest;
import com.notemind.user.dto.UpdateProfileRequest;
import com.notemind.user.vo.UserProfileVO;

/**
 * 用户中心服务。
 *
 * <p>userId 一律由调用方传入，不在 Service 内部从 SecurityContext 取 ——
 * 服务层不感知 Web / Security 上下文。
 */
public interface UserService {

    UserProfileVO getProfile(Long userId);

    /** 仅更新请求中非 null 的字段 */
    UserProfileVO updateProfile(Long userId, UpdateProfileRequest request);

    void updatePassword(Long userId, UpdatePasswordRequest request);
}
