package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.PageResult;
import com.jianzhang.common.Result;
import com.jianzhang.dto.FinanceDTO;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.entity.FinanceRecord;
import com.jianzhang.service.FinanceService;
import com.jianzhang.vo.FinanceStatVO;
import com.jianzhang.vo.ImportResultVO;
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
 * 财务中心接口
 * 权限策略：
 * - 查询类：管理员 / 财务 / 管理层 可访问（管理层只读）
 * - 写入类：管理员 / 财务，且需具备对应按钮权限
 */
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final FinanceService financeService;

    /**
     * 收支记录分页查询（支持类型、关键词、时间区间筛选）
     */
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','MANAGER')")
    @GetMapping("/page")
    public Result<PageResult<FinanceRecord>> page(RecordQuery query) {
        return Result.success(financeService.page(query));
    }

    /**
     * 财务统计报表
     */
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','MANAGER')")
    @GetMapping("/stat")
    public Result<FinanceStatVO> stat(@RequestParam(defaultValue = "6") Integer months) {
        return Result.success(financeService.stat(months));
    }

    /**
     * 导出收支明细
     */
    @Log(module = "财务中心", value = "导出收支数据")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','MANAGER') and hasAuthority('finance:record:export')")
    @GetMapping("/export")
    public void export(RecordQuery query, HttpServletResponse response) throws IOException {
        financeService.export(query, response);
    }

    /**
     * 下载导入模板
     */
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE') and hasAuthority('finance:record:import')")
    @GetMapping("/template")
    public void template(HttpServletResponse response) throws IOException {
        financeService.template(response);
    }

    /**
     * Excel 导入收支记录
     */
    @Log(module = "财务中心", value = "导入收支数据")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE') and hasAuthority('finance:record:import')")
    @PostMapping("/import")
    public Result<ImportResultVO> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
        ImportResultVO result = financeService.importExcel(file);
        return Result.success(result.summary(), result);
    }

    /**
     * 批量新增收支记录（前端可编辑表格）
     */
    @Log(module = "财务中心", value = "批量新增收支")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE') and hasAuthority('finance:record:batch')")
    @PostMapping("/batch")
    public Result<ImportResultVO> batch(@RequestBody List<FinanceDTO> list) {
        ImportResultVO result = financeService.batchAdd(list);
        return Result.success(result.summary(), result);
    }

    /**
     * 收支详情
     */
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE','MANAGER')")
    @GetMapping("/{id}")
    public Result<FinanceRecord> detail(@PathVariable Long id) {
        return Result.success(financeService.detail(id));
    }

    /**
     * 新增收支
     */
    @Log(module = "财务中心", value = "新增收支记录")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE') and hasAuthority('finance:record:add')")
    @PostMapping
    public Result<Void> add(@Valid @RequestBody FinanceDTO dto) {
        financeService.add(dto);
        return Result.success("新增成功", null);
    }

    /**
     * 编辑收支
     */
    @Log(module = "财务中心", value = "编辑收支记录")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE') and hasAuthority('finance:record:edit')")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody FinanceDTO dto) {
        financeService.update(dto);
        return Result.success("修改成功", null);
    }

    /**
     * 删除收支
     */
    @Log(module = "财务中心", value = "删除收支记录（移入回收站）")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCE') and hasAuthority('finance:record:remove')")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        financeService.delete(id);
        return Result.success("已移入回收站", null);
    }

    // ==================== 大额支出审批（管理层 / 管理员） ====================

    /**
     * 审批分页（默认待审批，可通过 auditStatus 查看已处理）
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('finance:audit:list')")
    @GetMapping("/audit/page")
    public Result<PageResult<FinanceRecord>> auditPage(RecordQuery query) {
        return Result.success(financeService.auditPage(query));
    }

    /**
     * 通过审批
     */
    @Log(module = "审批中心", value = "通过大额支出审批")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('finance:audit:agree')")
    @PutMapping("/audit/{id}/agree")
    public Result<Void> agree(@PathVariable Long id,
                              @RequestBody(required = false) java.util.Map<String, String> body) {
        financeService.agreeAudit(id, body == null ? null : body.get("remark"));
        return Result.success("已通过", null);
    }

    /**
     * 驳回审批
     */
    @Log(module = "审批中心", value = "驳回大额支出审批")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('finance:audit:reject')")
    @PutMapping("/audit/{id}/reject")
    public Result<Void> reject(@PathVariable Long id,
                               @RequestBody(required = false) java.util.Map<String, String> body) {
        financeService.rejectAudit(id, body == null ? null : body.get("remark"));
        return Result.success("已驳回，财务可修改后重新提交", null);
    }

    // ==================== 回收站（仅管理员） ====================

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:recycle:list')")
    @GetMapping("/recycle")
    public Result<PageResult<FinanceRecord>> recyclePage(@RequestParam(defaultValue = "1") Integer pageNum,
                                                         @RequestParam(defaultValue = "10") Integer pageSize,
                                                         @RequestParam(required = false) String keyword) {
        return Result.success(financeService.recyclePage(pageNum, pageSize, keyword));
    }

    @Log(module = "系统管理", value = "回收站恢复收支记录")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:recycle:restore')")
    @PutMapping("/recycle/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        financeService.restore(id);
        return Result.success("恢复成功", null);
    }

    @Log(module = "系统管理", value = "回收站彻底删除收支记录")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:recycle:purge')")
    @DeleteMapping("/recycle/{id}")
    public Result<Void> purge(@PathVariable Long id) {
        financeService.purge(id);
        return Result.success("已彻底删除", null);
    }
}
