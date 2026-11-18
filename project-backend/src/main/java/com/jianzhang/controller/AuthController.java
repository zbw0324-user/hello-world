package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.Result;
import com.jianzhang.dto.LoginDTO;
import com.jianzhang.security.JwtUtil;
import com.jianzhang.security.OnlineUserService;
import com.jianzhang.service.AuthService;
import com.jianzhang.util.IpUtil;
import com.jianzhang.vo.LoginVO;
import com.jianzhang.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

/**
 * 登录认证接口
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final OnlineUserService onlineUserService;

    /**
     * 账号密码登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        LoginVO vo = authService.login(dto);
        // 登记在线状态（同一账号旧会话会被顶替）
        onlineUserService.record(vo.getToken(), vo, IpUtil.getClientIp(request));
        return Result.success("登录成功", vo);
    }

    /**
     * 获取当前登录用户信息（含权限标识、动态菜单）
     */
    @GetMapping("/info")
    public Result<UserInfoVO> info() {
        return Result.success(authService.getInfo());
    }

    /**
     * 退出登录（JWT 无状态，前端清除令牌即可，此处仅记录日志）
     */
    @Log(module = "登录认证", value = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = jwtUtil.resolveToken(request);
        if (token != null) {
            onlineUserService.remove(token);
        }
        return Result.success("已退出登录", null);
    }
}
