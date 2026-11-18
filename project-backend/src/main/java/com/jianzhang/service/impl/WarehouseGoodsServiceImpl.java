package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianzhang.entity.WarehouseGoods;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.WarehouseGoodsMapper;
import com.jianzhang.service.WarehouseGoodsService;
import com.jianzhang.service.WarehouseService;
import com.jianzhang.vo.GoodsStockVO;
import com.jianzhang.vo.StockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品档案服务实现：档案 CRUD + 库存预警（台账实时库存联查）
 */
@Service
@RequiredArgsConstructor
public class WarehouseGoodsServiceImpl implements WarehouseGoodsService {

    private final WarehouseGoodsMapper goodsMapper;
    private final WarehouseService warehouseService;

    @Override
    public List<GoodsStockVO> list(String keyword) {
        List<WarehouseGoods> goods = selectGoods(keyword);
        return toVos(goods);
    }

    @Override
    public List<GoodsStockVO> warningList() {
        List<GoodsStockVO> vos = toVos(selectGoods(null));
        return vos.stream()
                .filter(vo -> vo.getStatus() != null && vo.getStatus() == 1)
                .filter(vo -> vo.getMinStock() != null && vo.getMinStock() > 0)
                .filter(vo -> (vo.getStock() == null ? 0 : vo.getStock()) < vo.getMinStock())
                .collect(Collectors.toList());
    }

    @Override
    public Long countWarning() {
        return (long) warningList().size();
    }

    @Override
    public List<WarehouseGoods> selectable() {
        return goodsMapper.selectList(new LambdaQueryWrapper<WarehouseGoods>()
                .eq(WarehouseGoods::getStatus, 1)
                .orderByAsc(WarehouseGoods::getGoodsName));
    }

    @Override
    public void add(WarehouseGoods goods) {
        checkGoods(goods);
        String name = goods.getGoodsName().trim();
        Long count = goodsMapper.selectCount(new LambdaQueryWrapper<WarehouseGoods>()
                .eq(WarehouseGoods::getGoodsName, name));
        if (count != null && count > 0) {
            throw new BusinessException("商品「" + name + "」已在档案中，请勿重复添加");
        }
        goods.setGoodsId(null);
        goods.setGoodsName(name);
        if (goods.getStatus() == null) {
            goods.setStatus(1);
        }
        goodsMapper.insert(goods);
    }

    @Override
    public void update(WarehouseGoods goods) {
        if (goods.getGoodsId() == null) {
            throw new BusinessException("商品ID不能为空");
        }
        WarehouseGoods exist = goodsMapper.selectById(goods.getGoodsId());
        if (exist == null) {
            throw new BusinessException("商品不存在或已被删除");
        }
        checkGoods(goods);
        String name = goods.getGoodsName().trim();
        if (!name.equals(exist.getGoodsName())) {
            Long count = goodsMapper.selectCount(new LambdaQueryWrapper<WarehouseGoods>()
                    .eq(WarehouseGoods::getGoodsName, name));
            if (count != null && count > 0) {
                throw new BusinessException("商品「" + name + "」已在档案中，请勿重复添加");
            }
        }
        goods.setGoodsName(name);
        if (goods.getStatus() == null) {
            goods.setStatus(exist.getStatus());
        }
        goodsMapper.updateById(goods);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WarehouseGoods exist = goodsMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("商品不存在或已被删除");
        }
        // 有待审批领用申请的商品不可删除，避免审批时找不到商品
        Integer stock = currentStock(exist.getGoodsName());
        if (stock != null && stock > 0) {
            throw new BusinessException("商品「" + exist.getGoodsName() + "」仍有库存（" + stock + "），建议停用而非删除");
        }
        goodsMapper.deleteById(id);
    }

    // ================= 内部方法 =================

    private List<WarehouseGoods> selectGoods(String keyword) {
        LambdaQueryWrapper<WarehouseGoods> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(WarehouseGoods::getGoodsName, keyword)
                    .or().like(WarehouseGoods::getCategory, keyword));
        }
        wrapper.orderByDesc(WarehouseGoods::getStatus)
                .orderByAsc(WarehouseGoods::getGoodsName);
        return goodsMapper.selectList(wrapper);
    }

    /** 档案 + 台账实时库存合并 */
    private List<GoodsStockVO> toVos(List<WarehouseGoods> goods) {
        Map<String, Integer> stockMap = new HashMap<>();
        for (StockVO stock : warehouseService.stockList(null)) {
            stockMap.put(stock.getGoodsName(), stock.getStock());
        }
        List<GoodsStockVO> vos = new ArrayList<>();
        for (WarehouseGoods item : goods) {
            GoodsStockVO vo = new GoodsStockVO();
            vo.setGoodsId(item.getGoodsId());
            vo.setGoodsName(item.getGoodsName());
            vo.setCategory(item.getCategory());
            vo.setUnit(item.getUnit());
            vo.setPrice(item.getPrice());
            vo.setMinStock(item.getMinStock());
            vo.setStatus(item.getStatus());
            vo.setRemark(item.getRemark());
            vo.setStock(stockMap.getOrDefault(item.getGoodsName(), 0));
            vo.setCreateTime(item.getCreateTime());
            vo.setUpdateTime(item.getUpdateTime());
            vos.add(vo);
        }
        return vos;
    }

    private Integer currentStock(String goodsName) {
        return warehouseService.stockList(null).stream()
                .filter(s -> goodsName.equals(s.getGoodsName()))
                .map(StockVO::getStock)
                .findFirst()
                .orElse(0);
    }

    private void checkGoods(WarehouseGoods goods) {
        if (!StringUtils.hasText(goods.getGoodsName())) {
            throw new BusinessException("商品名称不能为空");
        }
        if (goods.getGoodsName().length() > 100) {
            throw new BusinessException("商品名称不能超过 100 个字符");
        }
        if (goods.getMinStock() == null || goods.getMinStock() < 0) {
            goods.setMinStock(0);
        }
        if (goods.getPrice() == null) {
            goods.setPrice(BigDecimal.ZERO);
        }
        if (goods.getPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("参考单价不能为负数");
        }
        if (goods.getPrice().compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new BusinessException("参考单价超出上限");
        }
        if (!StringUtils.hasText(goods.getCategory())) {
            goods.setCategory("未分类");
        }
        if (!StringUtils.hasText(goods.getUnit())) {
            goods.setUnit("件");
        }
    }
}
