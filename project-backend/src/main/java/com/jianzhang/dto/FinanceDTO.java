package com.jianzhang.dto;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 财务收支新增 / 编辑入参
 */
@Data
public class FinanceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "请选择收支类型")
    private Integer type;

    @NotNull(message = "请输入金额")
    @DecimalMin(value = "0.01", message = "金额必须大于 0")
    private BigDecimal amount;

    @NotBlank(message = "请输入收支来源/用途")
    private String source;

    /** 收支时间 yyyy-MM-dd HH:mm:ss */
    @NotBlank(message = "请选择收支时间")
    private String recordTime;

    private String remark;
}
