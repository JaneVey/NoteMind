package com.notemind.user.service.impl;

import com.notemind.common.exception.BusinessException;
import com.notemind.common.exception.ErrorCode;
import com.notemind.user.dto.UpdatePasswordRequest;
import com.notemind.user.dto.UpdateProfileRequest;
import com.notemind.user.entity.SysUser;
import com.notemind.user.mapper.SysUserMapper;
import com.notemind.user.service.UserService;
import com.notemind.user.vo.UserProfileVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户中心服务实现。
 *
 * <p><b>变更记录（2026-10-08）</b>：业务逻辑此前直接写在 {@code UserController} 里
 * 并让它注入了 {@code SysUserMapper} —— 违反《后端开发规范》：
 * Controller 只做「校验参数 + 调 Service + 包装响应」，禁止注入 Mapper。
 * 现逻辑下沉到本类，Controller 只依赖 {@link UserService}。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public UserProfileVO getProfile(Long userId) {
        return toVO(requireUser(userId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateProfile(Long userId, UpdateProfileRequest request) {
        SysUser user = requireUser(userId);

        // 只更新显式传入的字段，避免把未传字段覆盖成 null
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname());
        }
        if (request.getAvatar() != null) {
            user.setAvatar(request.getAvatar());
        }

        sysUserMapper.updateById(user);
        return toVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordRequest request) {
        SysUser user = requireUser(userId);

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.OLD_PASSWORD_WRONG);
        }
        if (request.getOldPassword().equals(request.getNewPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_UNCHANGED);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        sysUserMapper.updateById(user);

        // 只记 userId，绝不记密码相关内容
        log.info("用户修改密码: userId={}", userId);
    }

    /** 按 userId 取用户，不存在即抛业务异常 */
    private SysUser requireUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        return user;
    }

    /** Entity -> VO，禁止把 Entity 直接返回给前端（含 password 字段） */
    private UserProfileVO toVO(SysUser user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setUserId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
