package com.jianzhang.service;

import com.jianzhang.entity.WarehouseGoods;
import com.jianzhang.vo.GoodsStockVO;

import java.util.List;

/**
 * 商品档案服务：档案维护 + 库存预警
 */
public interface WarehouseGoodsService {

    /**
     * 商品档案列表（keyword 匹配名称/分类，附带当前库存）
     */
    List<GoodsStockVO> list(String keyword);

    /**
     * 库存预警列表（在册且当前库存低于预警阈值）
     */
    List<GoodsStockVO> warningList();

    /**
     * 预警商品数量（看板用）
     */
    Long countWarning();

    /**
     * 可领用商品列表（在册商品，员工提交领用申请时的下拉数据）
     */
    List<WarehouseGoods> selectable();

    void add(WarehouseGoods goods);

    void update(WarehouseGoods goods);

    void delete(Long id);
}
