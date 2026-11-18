package com.jianzhang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 菜单/权限表 sys_menu
 */
@Data
@TableName("sys_menu")
public class SysMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "menu_id", type = IdType.AUTO)
    private Long menuId;

    /** 父级ID，0为顶级 */
    private Long parentId;

    /** 菜单名称 */
    private String menuName;

    /** 路由地址 */
    private String path;

    /** 前端组件路径 */
    private String component;

    /** 菜单图标 */
    private String icon;

    /** 排序 */
    private Integer sort;

    /** 类型 1目录 2菜单 3按钮 */
    private Integer menuType;

    /** 权限标识 */
    private String perms;

    /** 是否显示 0隐藏 1显示 */
    private Integer visible;

    /** 子菜单（非数据库字段） */
    @TableField(exist = false)
    private List<SysMenu> children = new ArrayList<>();
}
