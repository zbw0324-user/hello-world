package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 登录成功返回数据
 */
@Data
public class LoginVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** JWT 令牌 */
    private String token;
    /** 令牌前缀 */
    private String tokenPrefix = "Bearer ";
    /** 有效期（秒） */
    private Long expire;
    private Long userId;
    private String username;
    private String nickname;
    private Long roleId;
    private String roleCode;
    private String roleName;
}
