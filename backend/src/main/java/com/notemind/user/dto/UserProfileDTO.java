package com.notemind.user.dto;

import lombok.Data;

@Data
public class UserProfileDTO {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String avatar;
}
