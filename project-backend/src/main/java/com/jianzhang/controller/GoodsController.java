package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.Result;
import com.jianzhang.entity.WarehouseGoods;
import com.jianzhang.service.WarehouseGoodsService;
import com.jianzhang.vo.GoodsStockVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 商品档案接口
 * 权限策略：
 * - 查询类：管理员 / 仓库 / 管理层 可查看（管理层只读）
 * - 维护类：管理员 / 仓库
 */
@RestController
@RequestMapping("/api/warehouse/goods")
@RequiredArgsConstructor
public class GoodsController {

    private final WarehouseGoodsService goodsService;

    /**
     * 商品档案列表（附带当前库存）
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER') and hasAuthority('warehouse:goods:list')")
    @GetMapping
    public Result<List<GoodsStockVO>> list(@RequestParam(required = false) String keyword) {
        return Result.success(goodsService.list(keyword));
    }

    /**
     * 库存预警列表
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER') and hasAuthority('warehouse:goods:list')")
    @GetMapping("/warning")
    public Result<List<GoodsStockVO>> warning() {
        return Result.success(goodsService.warningList());
    }

    /**
     * 新增商品档案
     */
    @Log(module = "仓库中心", value = "新增商品档案")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:goods:add')")
    @PostMapping
    public Result<Void> add(@RequestBody WarehouseGoods goods) {
        goodsService.add(goods);
        return Result.success("新增成功", null);
    }

    /**
     * 编辑商品档案
     */
    @Log(module = "仓库中心", value = "编辑商品档案")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:goods:edit')")
    @PutMapping
    public Result<Void> update(@RequestBody WarehouseGoods goods) {
        goodsService.update(goods);
        return Result.success("修改成功", null);
    }

    /**
     * 删除商品档案（有库存时禁止）
     */
    @Log(module = "仓库中心", value = "删除商品档案")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:goods:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        goodsService.delete(id);
        return Result.success("删除成功", null);
    }
}
