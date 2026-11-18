package com.jianzhang.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 用户新增 / 编辑入参
 */
@Data
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    @NotBlank(message = "登录账号不能为空")
    @Size(min = 3, max = 20, message = "登录账号长度需为 3-20 位")
    private String username;

    /** 新增时必填，编辑时留空表示不修改 */
    private String password;

    @NotBlank(message = "用户昵称不能为空")
    private String nickname;

    @NotNull(message = "请选择所属角色")
    private Long roleId;

    /** 账号状态 0禁用 1正常 */
    private Integer status = 1;

    private String remark;
}
