package com.jianzhang.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统操作日志表 sys_log
 */
@Data
@TableName("sys_log")
public class SysLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /** 操作账号 */
    private String username;

    /** 角色标识 */
    private String roleCode;

    /** 所属模块 */
    private String module;

    /** 操作内容 */
    private String operation;

    /** 请求方法 */
    private String method;

    /** 请求地址 */
    private String requestUri;

    /** 请求参数 */
    private String params;

    /** 操作IP */
    private String ip;

    /** 耗时(ms) */
    private Long costTime;

    /** 状态 0失败 1成功 */
    private Integer status;

    /** 操作时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
