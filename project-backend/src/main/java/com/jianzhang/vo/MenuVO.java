package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树（前端动态路由 / 侧边栏使用）
 */
@Data
public class MenuVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long menuId;
    private Long parentId;
    private String menuName;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private Integer menuType;
    private String perms;
    private List<MenuVO> children = new ArrayList<>();
}
