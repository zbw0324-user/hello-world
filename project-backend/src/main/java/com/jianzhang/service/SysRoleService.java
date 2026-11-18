package com.jianzhang.service;

import com.jianzhang.dto.RoleDTO;
import com.jianzhang.entity.SysRole;

import java.util.List;

/**
 * 角色与权限分配服务
 */
public interface SysRoleService {

    List<SysRole> list();

    void add(RoleDTO dto);

    void update(RoleDTO dto);

    void delete(Long roleId);

    /**
     * 角色已授权的菜单ID（授权回显）
     */
    List<Long> menuIds(Long roleId);

    /**
     * 重新分配角色菜单权限
     */
    void assignMenus(RoleDTO dto);
}
