package com.jianzhang.service;

import com.jianzhang.dto.LoginDTO;
import com.jianzhang.dto.PasswordDTO;
import com.jianzhang.vo.LoginVO;
import com.jianzhang.vo.UserInfoVO;

/**
 * 登录认证服务
 */
public interface AuthService {

    /**
     * 账号密码登录，返回 JWT 令牌
     */
    LoginVO login(LoginDTO dto);

    /**
     * 获取当前登录用户信息（含权限、动态菜单）
     */
    UserInfoVO getInfo();

    /**
     * 修改当前登录用户密码
     */
    void changePassword(PasswordDTO dto);
}
