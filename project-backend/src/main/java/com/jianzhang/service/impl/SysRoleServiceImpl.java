package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianzhang.dto.RoleDTO;
import com.jianzhang.entity.SysMenu;
import com.jianzhang.entity.SysRole;
import com.jianzhang.entity.SysUser;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.SysMenuMapper;
import com.jianzhang.mapper.SysRoleMapper;
import com.jianzhang.mapper.SysUserMapper;
import com.jianzhang.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 角色与权限分配服务实现
 */
@Service
@RequiredArgsConstructor
public class SysRoleServiceImpl implements SysRoleService {

    private static final String ADMIN_ROLE_CODE = "admin";

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysMenuMapper menuMapper;

    @Override
    public List<SysRole> list() {
        List<SysRole> roles = roleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .orderByAsc(SysRole::getRoleId));
        // 统计每个角色下的用户数量
        for (SysRole role : roles) {
            Long count = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getRoleId, role.getRoleId()));
            role.setUserCount(count == null ? 0 : count.intValue());
        }
        return roles;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(RoleDTO dto) {
        checkCodeUnique(dto.getRoleCode(), null);
        SysRole role = new SysRole();
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleCode(dto.getRoleCode().trim().toLowerCase());
        roleMapper.insert(role);
        saveMenus(role.getRoleId(), dto.getMenuIds());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(RoleDTO dto) {
        if (dto.getRoleId() == null) {
            throw new BusinessException("缺少角色ID");
        }
        SysRole exist = roleMapper.selectById(dto.getRoleId());
        if (exist == null) {
            throw new BusinessException("角色不存在");
        }
        checkCodeUnique(dto.getRoleCode(), dto.getRoleId());
        if (ADMIN_ROLE_CODE.equals(exist.getRoleCode())
                && !ADMIN_ROLE_CODE.equals(dto.getRoleCode().trim().toLowerCase())) {
            throw new BusinessException("超级管理员角色标识不允许修改");
        }
        SysRole role = new SysRole();
        role.setRoleId(dto.getRoleId());
        role.setRoleName(dto.getRoleName().trim());
        role.setRoleCode(dto.getRoleCode().trim().toLowerCase());
        roleMapper.updateById(role);
        if (dto.getMenuIds() != null) {
            saveMenus(dto.getRoleId(), dto.getMenuIds());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if (ADMIN_ROLE_CODE.equals(role.getRoleCode())) {
            throw new BusinessException("超级管理员角色不允许删除");
        }
        Long userCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, roleId));
        if (userCount != null && userCount > 0) {
            throw new BusinessException("该角色下还有 " + userCount + " 个用户，无法删除");
        }
        roleMapper.deleteById(roleId);
        menuMapper.deleteRoleMenu(roleId);
    }

    @Override
    public List<Long> menuIds(Long roleId) {
        return menuMapper.selectMenuIdsByRoleId(roleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignMenus(RoleDTO dto) {
        if (dto.getRoleId() == null) {
            throw new BusinessException("缺少角色ID");
        }
        SysRole role = roleMapper.selectById(dto.getRoleId());
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        saveMenus(dto.getRoleId(), dto.getMenuIds());
    }

    /**
     * 覆盖式保存角色菜单权限（自动补齐父级菜单，并去重，避免出现孤儿节点与主键冲突）
     */
    private void saveMenus(Long roleId, List<Long> menuIds) {
        menuMapper.deleteRoleMenu(roleId);
        if (menuIds == null || menuIds.isEmpty()) {
            return;
        }
        Set<Long> granted = new LinkedHashSet<>();
        for (Long menuId : menuIds) {
            if (menuId == null) {
                continue;
            }
            granted.add(menuId);
            // 逐级向上补齐父菜单，保证侧边栏能正常渲染
            Long parentId = menuId;
            int depth = 0;
            while (parentId != null && parentId != 0L && depth++ < 10) {
                SysMenu menu = menuMapper.selectById(parentId);
                if (menu == null) {
                    break;
                }
                parentId = menu.getParentId();
                if (parentId != null && parentId != 0L) {
                    granted.add(parentId);
                }
            }
        }
        for (Long id : granted) {
            menuMapper.insertRoleMenu(roleId, id);
        }
    }

    private void checkCodeUnique(String roleCode, Long excludeId) {
        Long count = roleMapper.selectCount(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, roleCode.trim().toLowerCase())
                .ne(excludeId != null, SysRole::getRoleId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException("角色标识已存在，请更换");
        }
    }
}
