package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品档案视图（档案信息 + 当前库存）
 */
@Data
public class GoodsStockVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long goodsId;

    /** 商品名称 */
    private String goodsName;

    /** 分类 */
    private String category;

    /** 计量单位 */
    private String unit;

    /** 参考单价 */
    private BigDecimal price;

    /** 库存预警阈值 */
    private Integer minStock;

    /** 状态 1在册 0停用 */
    private Integer status;

    private String remark;

    /** 当前库存（由出入库台账实时汇总） */
    private Integer stock;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
