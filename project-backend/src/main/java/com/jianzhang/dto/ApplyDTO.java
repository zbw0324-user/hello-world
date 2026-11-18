package com.jianzhang.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

/**
 * 领用申请提交入参
 */
@Data
public class ApplyDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 领用商品名称 */
    @NotBlank(message = "请选择领用商品")
    private String goodsName;

    /** 领用数量 */
    @NotNull(message = "请输入领用数量")
    @Min(value = 1, message = "领用数量必须大于 0")
    private Integer applyNum;

    /** 领用事由 */
    @NotBlank(message = "请填写领用事由")
    @Size(max = 255, message = "领用事由不能超过 255 个字符")
    private String reason;
}
