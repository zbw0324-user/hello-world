package com.jianzhang.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户表 sys_user
 */
@Data
@TableName("sys_user")
public class SysUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;

    /** 登录账号（唯一） */
    private String username;

    /** 加密密码（BCrypt），不返回给前端 */
    @JsonIgnore
    private String password;

    /** 用户昵称 */
    private String nickname;

    /** 关联角色ID */
    private Long roleId;

    /** 账号状态 0禁用 1正常 */
    private Integer status;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 备注 */
    private String remark;

    /** 角色名称（非数据库字段） */
    @TableField(exist = false)
    private String roleName;

    /** 角色标识（非数据库字段） */
    @TableField(exist = false)
    private String roleCode;
}
