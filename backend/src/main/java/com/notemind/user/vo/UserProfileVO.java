package com.notemind.user.vo;

import lombok.Data;

@Data
public class UserProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
}
