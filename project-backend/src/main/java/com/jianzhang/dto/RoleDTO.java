package com.jianzhang.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.List;

/**
 * 角色新增 / 编辑 / 授权入参
 */
@Data
public class RoleDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long roleId;

    @NotBlank(message = "角色名称不能为空")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    private String roleCode;

    /** 授权的菜单ID集合（含按钮） */
    private List<Long> menuIds;
}
