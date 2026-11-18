package com.jianzhang.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工领用申请表 staff_apply
 */
@Data
@TableName("staff_apply")
public class StaffApply implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 申请ID */
    @TableId(value = "apply_id", type = IdType.AUTO)
    private Long applyId;

    /** 申请人用户ID */
    private Long userId;

    /** 申请人账号 */
    private String username;

    /** 申请人姓名 */
    private String nickname;

    /** 领用商品名称 */
    private String goodsName;

    /** 领用数量 */
    private Integer applyNum;

    /** 领用事由 */
    private String reason;

    /** 状态 0待审批 1已通过 2已驳回 3已撤回 */
    private Integer status;

    /** 通过后生成的出库单ID */
    private Long recordId;

    /** 审批人 */
    private String auditBy;

    /** 审批时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    /** 审批意见 */
    private String auditRemark;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
