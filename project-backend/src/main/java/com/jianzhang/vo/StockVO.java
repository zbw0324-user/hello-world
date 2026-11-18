package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 库存台账行数据
 */
@Data
public class StockVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品名称 */
    private String goodsName;
    /** 累计入库 */
    private Integer inNum;
    /** 累计出库 */
    private Integer outNum;
    /** 当前库存 */
    private Integer stock;
    /** 最近一次出入库时间 */
    private String lastTime;
}
