package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianzhang.entity.SysMenu;
import com.jianzhang.mapper.SysMenuMapper;
import com.jianzhang.service.SysMenuService;
import com.jianzhang.vo.MenuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单权限服务实现
 */
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl implements SysMenuService {

    private final SysMenuMapper menuMapper;

    @Override
    public List<MenuVO> getMenuTreeByRole(Long roleId) {
        List<SysMenu> menus = menuMapper.selectByRoleId(roleId);
        // 只保留目录与菜单，且为显示状态
        List<SysMenu> visibleMenus = menus.stream()
                .filter(m -> m.getMenuType() != null && m.getMenuType() != 3)
                .filter(m -> m.getVisible() == null || m.getVisible() == 1)
                .collect(Collectors.toList());
        return buildTree(visibleMenus);
    }

    @Override
    public List<String> getPermsByRole(Long roleId) {
        return menuMapper.selectByRoleId(roleId).stream()
                .map(SysMenu::getPerms)
                .filter(StringUtils::hasText)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public List<MenuVO> getAllMenuTree() {
        List<SysMenu> menus = menuMapper.selectList(new LambdaQueryWrapper<SysMenu>()
                .orderByAsc(SysMenu::getSort)
                .orderByAsc(SysMenu::getMenuId));
        return buildTree(menus);
    }

    @Override
    public List<SysMenu> listByRole(Long roleId) {
        return menuMapper.selectByRoleId(roleId);
    }

    /**
     * 平铺列表 -> 树结构
     */
    private List<MenuVO> buildTree(List<SysMenu> menus) {
        Map<Long, MenuVO> nodeMap = new LinkedHashMap<>();
        for (SysMenu menu : menus) {
            nodeMap.put(menu.getMenuId(), toVO(menu));
        }
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : nodeMap.values()) {
            Long parentId = node.getParentId();
            if (parentId == null || parentId == 0L || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                nodeMap.get(parentId).getChildren().add(node);
            }
        }
        // 清理按钮节点的空 children，保持前端 JSON 干净
        roots.forEach(this::cleanChildren);
        return roots;
    }

    private void cleanChildren(MenuVO node) {
        if (node.getChildren() == null || node.getChildren().isEmpty()) {
            node.setChildren(null);
            return;
        }
        node.getChildren().forEach(this::cleanChildren);
    }

    private MenuVO toVO(SysMenu menu) {
        MenuVO vo = new MenuVO();
        vo.setMenuId(menu.getMenuId());
        vo.setParentId(menu.getParentId());
        vo.setMenuName(menu.getMenuName());
        vo.setPath(menu.getPath());
        vo.setComponent(menu.getComponent());
        vo.setIcon(menu.getIcon());
        vo.setSort(menu.getSort());
        vo.setMenuType(menu.getMenuType());
        vo.setPerms(menu.getPerms());
        return vo;
    }
}
