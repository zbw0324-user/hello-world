package com.jianzhang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jianzhang.entity.SysMenu;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 菜单/权限 Mapper
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 查询某角色被授权的全部菜单（含目录、菜单、按钮）
     */
    @Select("SELECT m.* FROM sys_menu m " +
            "INNER JOIN sys_role_menu rm ON rm.menu_id = m.menu_id " +
            "WHERE rm.role_id = #{roleId} ORDER BY m.sort ASC, m.menu_id ASC")
    List<SysMenu> selectByRoleId(@Param("roleId") Long roleId);

    /**
     * 查询某角色已授权的菜单ID集合（用于角色授权回显）
     */
    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRoleId(@Param("roleId") Long roleId);

    @Delete("DELETE FROM sys_role_menu WHERE role_id = #{roleId}")
    int deleteRoleMenu(@Param("roleId") Long roleId);

    @Insert("INSERT INTO sys_role_menu(role_id, menu_id) VALUES(#{roleId}, #{menuId})")
    int insertRoleMenu(@Param("roleId") Long roleId, @Param("menuId") Long menuId);
}
