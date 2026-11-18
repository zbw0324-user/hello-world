package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.Result;
import com.jianzhang.security.OnlineUser;
import com.jianzhang.security.OnlineUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 在线用户管理接口
 */
@RestController
@RequestMapping("/api/admin/online")
@RequiredArgsConstructor
public class OnlineUserController {

    private final OnlineUserService onlineUserService;

    /**
     * 在线用户列表
     */
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:online:list')")
    @GetMapping
    public Result<List<OnlineUser>> list() {
        return Result.success(onlineUserService.list());
    }

    /**
     * 强制下线
     */
    @Log(module = "系统管理", value = "强制下线在线用户")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:online:kick')")
    @DeleteMapping("/{tokenId}")
    public Result<Void> kick(@PathVariable String tokenId) {
        boolean ok = onlineUserService.kick(tokenId);
        return ok ? Result.success("已强制下线", null) : Result.error("该会话已不在线");
    }
}
