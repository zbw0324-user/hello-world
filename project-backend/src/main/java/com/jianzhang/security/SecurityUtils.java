package com.jianzhang.security;

import com.jianzhang.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户工具类
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前登录用户，未登录抛 401
     */
    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        throw new BusinessException(401, "登录状态已失效，请重新登录");
    }

    /**
     * 获取当前登录用户（可能为空）
     */
    public static LoginUser getLoginUserOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser) {
            return (LoginUser) authentication.getPrincipal();
        }
        return null;
    }

    public static Long getUserId() {
        return getLoginUser().getUserId();
    }

    public static String getUsername() {
        return getLoginUser().getUsername();
    }

    public static String getRoleCode() {
        return getLoginUser().getRoleCode();
    }

    /**
     * 是否拥有某个权限标识
     */
    public static boolean hasPerm(String perm) {
        LoginUser user = getLoginUserOrNull();
        return user != null && user.getPerms() != null && user.getPerms().contains(perm);
    }
}
