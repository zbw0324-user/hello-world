package com.jianzhang.dto;

import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 仓库出入库新增 / 编辑入参
 */
@Data
public class WarehouseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotNull(message = "请选择单据类型")
    private Integer type;

    @NotBlank(message = "请输入商品名称")
    private String goodsName;

    @NotNull(message = "请输入数量")
    @Min(value = 1, message = "数量必须大于 0")
    private Integer goodsNum;

    /** 出入库时间 yyyy-MM-dd HH:mm:ss */
    @NotBlank(message = "请选择出入库时间")
    private String recordTime;

    private String remark;
}
