package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.Result;
import com.jianzhang.dto.PasswordDTO;
import com.jianzhang.service.AuthService;
import com.jianzhang.vo.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 个人中心接口
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final AuthService authService;

    /**
     * 个人资料
     */
    @GetMapping("/profile")
    public Result<UserInfoVO> profile() {
        return Result.success(authService.getInfo());
    }

    /**
     * 修改密码
     */
    @Log(module = "个人中心", value = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        authService.changePassword(dto);
        return Result.success("密码修改成功，请重新登录", null);
    }
}
