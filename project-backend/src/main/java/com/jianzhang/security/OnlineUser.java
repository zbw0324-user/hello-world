package com.jianzhang.security;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 在线用户信息（仅存在于内存，不落库）
 */
@Data
public class OnlineUser {

    /** 在线会话标识（令牌摘要，用于强制下线定位） */
    private String tokenId;

    /** 登录令牌（脱敏后返回前端） */
    private String token;

    /** 登录账号 */
    private String username;

    /** 用户昵称 */
    private String nickname;

    /** 角色名称 */
    private String roleName;

    /** 登录IP */
    private String ip;

    /** 登录时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime loginTime;

    /** 令牌过期时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime expireTime;
}
