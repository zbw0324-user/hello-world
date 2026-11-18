package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 当前登录用户信息（含权限标识、动态菜单）
 */
@Data
public class UserInfoVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;
    private String username;
    private String nickname;
    private Long roleId;
    private String roleCode;
    private String roleName;
    private String remark;
    /** 按钮级权限标识集合 */
    private List<String> perms = new ArrayList<>();
    /** 动态菜单树 */
    private List<MenuVO> menus = new ArrayList<>();
}
