package com.notemind.user.service;

import com.notemind.user.dto.UpdatePasswordRequest;
import com.notemind.user.dto.UpdateProfileRequest;
import com.notemind.user.vo.UserProfileVO;

/**
 * 用户中心服务。
 *
 * <p>注意：所有方法都接收 {@code userId} 作为第一个参数，而不是在 Service 内部
 * 从 SecurityContext 取用户。这样做的原因：
 * <ol>
 *   <li>Service 层不感知 Web/Security 上下文，符合《后端开发规范》一节的"服务层禁止出现 Web 概念"；</li>
 *   <li>便于单元测试（直接传入任意 userId，不需要伪造 SecurityContext）；</li>
 *   <li>将来若被其他模块（如定时任务、管理后台）复用，不必依赖 HTTP 请求上下文。</li>
 * </ol>
 */
public interface UserService {

    /** 查询个人资料 */
    UserProfileVO getProfile(Long userId);

    /** 修改个人资料（仅更新请求中非 null 的字段） */
    UserProfileVO updateProfile(Long userId, UpdateProfileRequest request);

    /** 修改密码 */
    void updatePassword(Long userId, UpdatePasswordRequest request);
}
