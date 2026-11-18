package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.PageResult;
import com.jianzhang.common.Result;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.dto.WarehouseDTO;
import com.jianzhang.entity.WarehouseRecord;
import com.jianzhang.service.WarehouseService;
import com.jianzhang.vo.ImportResultVO;
import com.jianzhang.vo.StockVO;
import com.jianzhang.vo.WarehouseStatVO;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

/**
 * 仓库中心接口
 * 权限策略：
 * - 查询类：管理员 / 仓库 / 管理层 / 员工（仅台账）可访问
 * - 写入类：管理员 / 仓库，且需具备对应按钮权限
 */
@RestController
@RequestMapping("/api/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    /**
     * 出入库记录分页查询
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER')")
    @GetMapping("/page")
    public Result<PageResult<WarehouseRecord>> page(RecordQuery query) {
        return Result.success(warehouseService.page(query));
    }

    /**
     * 库存台账（按商品汇总）—— 员工角色也可查看
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER','STAFF')")
    @GetMapping("/stock")
    public Result<List<StockVO>> stock(@RequestParam(required = false) String keyword) {
        return Result.success(warehouseService.stockList(keyword));
    }

    /**
     * 商品名称列表（表单下拉提示）
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    @GetMapping("/goods-names")
    public Result<List<String>> goodsNames() {
        return Result.success(warehouseService.goodsNames());
    }

    /**
     * 库存统计报表
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER')")
    @GetMapping("/stat")
    public Result<WarehouseStatVO> stat(@RequestParam(defaultValue = "6") Integer months) {
        return Result.success(warehouseService.stat(months));
    }

    /**
     * 导出出入库明细
     */
    @Log(module = "仓库中心", value = "导出出入库数据")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER') and hasAuthority('warehouse:record:export')")
    @GetMapping("/export")
    public void export(RecordQuery query, HttpServletResponse response) throws IOException {
        warehouseService.export(query, response);
    }

    /**
     * 下载导入模板
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:record:import')")
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        warehouseService.template(response);
    }

    /**
     * Excel 导入出入库单据
     */
    @Log(module = "仓库中心", value = "导入出入库数据")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:record:import')")
    @PostMapping("/import")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        ImportResultVO result = warehouseService.importExcel(file);
        return Result.success(result.summary(), result);
    }

    /**
     * 批量新增单据（前端可编辑表格）
     */
    @Log(module = "仓库中心", value = "批量新增出入库")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:record:batch')")
    @PostMapping("/batch")
    public Result<ImportResultVO> batch(@RequestBody List<WarehouseDTO> list) {
        ImportResultVO result = warehouseService.batchAdd(list);
        return Result.success(result.summary(), result);
    }

    /**
     * 单据详情
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER')")
    @GetMapping("/{id}")
    public Result<WarehouseRecord> detail(@PathVariable Long id) {
        return Result.success(warehouseService.detail(id));
    }

    /**
     * 新增出入库单据
     */
    @Log(module = "仓库中心", value = "新增出入库单据")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:record:add')")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody WarehouseDTO dto) {
        warehouseService.add(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 编辑出入库单据
     */
    @Log(module = "仓库中心", value = "编辑出入库单据")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:record:edit')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody WarehouseDTO dto) {
        warehouseService.update(dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除出入库单据
     */
    @Log(module = "仓库中心", value = "删除出入库单据")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE') and hasAuthority('warehouse:record:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.success("已移入回收站", null);
    }

    // ==================== 回收站（仅管理员） ====================

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:recycle:list')")
    @GetMapping("/recycle")
    public Result<PageResult<WarehouseRecord>> recyclePage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                                           @RequestParam(required = false) String keyword) {
        return Result.success(warehouseService.recyclePage(pageNum, pageSize, keyword));
    }

    @Log(module = "系统管理", value = "回收站恢复出入库单据")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:recycle:restore')")
    @PutMapping("/recycle/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        warehouseService.restore(id);
        return Result.success("恢复成功", null);
    }

    @Log(module = "系统管理", value = "回收站彻底删除出入库单据")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:recycle:purge')")
    @DeleteMapping("/recycle/{id}")
    public Result<Void> purge(@PathVariable Long id) {
        warehouseService.purge(id);
        return Result.success("已彻底删除", null);
    }
}
