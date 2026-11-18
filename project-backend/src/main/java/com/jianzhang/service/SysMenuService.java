package com.jianzhang.service;

import com.jianzhang.entity.SysMenu;
import com.jianzhang.vo.MenuVO;

import java.util.List;

/**
 * 菜单权限服务
 */
public interface SysMenuService {

    /**
     * 根据角色获取菜单树（仅目录 + 菜单，用于动态路由与侧边栏）
     */
    List<MenuVO> getMenuTreeByRole(Long roleId);

    /**
     * 根据角色获取权限标识集合
     */
    List<String> getPermsByRole(Long roleId);

    /**
     * 获取全部菜单树（含按钮，用于角色授权）
     */
    List<MenuVO> getAllMenuTree();

    /**
     * 获取角色的全部菜单实体（含按钮）
     */
    List<SysMenu> listByRole(Long roleId);
}
