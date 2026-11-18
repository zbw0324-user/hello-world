package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 仓库统计数据
 */
@Data
public class WarehouseStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 累计入库数量 */
    private Integer totalInNum = 0;
    /** 累计出库数量 */
    private Integer totalOutNum = 0;
    /** 商品种类数 */
    private Integer goodsKinds = 0;
    /** 当前总库存 */
    private Integer totalStock = 0;
    /** 近 N 月出入库趋势 */
    private List<Map<String, Object>> trend = new ArrayList<>();
    /** 库存台账 */
    private List<StockVO> stocks = new ArrayList<>();
}
