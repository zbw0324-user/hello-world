package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.PageResult;
import com.jianzhang.common.Result;
import com.jianzhang.dto.ApplyDTO;
import com.jianzhang.entity.StaffApply;
import com.jianzhang.service.StaffApplyService;
import com.jianzhang.service.WarehouseGoodsService;
import com.jianzhang.entity.WarehouseGoods;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 员工领用申请接口
 * - 我的申请 / 提交 / 撤回：所有登录用户（员工为主）
 * - 审批列表 / 通过 / 驳回：仓库角色（管理层与管理员亦可审批）
 */
@RestController
@RequestMapping("/api/apply")
@RequiredArgsConstructor
public class ApplyController {

    private final StaffApplyService applyService;
    private final WarehouseGoodsService goodsService;

    /**
     * 可领用商品列表（在册商品）
     */
    @GetMapping("/goods")
    public Result<List<WarehouseGoods>> selectableGoods() {
        return Result.success(goodsService.selectable());
    }

    /**
     * 我的申请分页
     */
    @GetMapping("/my")
    public Result<PageResult<StaffApply>> my(@RequestParam(defaultValue = "1") Integer pageNum,
                                             @RequestParam(defaultValue = "10") Integer pageSize,
                                             @RequestParam(required = false) Integer status) {
        return Result.success(applyService.myPage(pageNum, pageSize, status));
    }

    /**
     * 提交领用申请
     */
    @Log(module = "领用中心", value = "提交领用申请")
    @PostMapping
    public Result<Void> submit(@Valid @RequestBody ApplyDTO dto) {
        applyService.submit(dto);
        return Result.success("申请已提交，请等待仓库管理员审批", null);
    }

    /**
     * 撤回申请
     */
    @Log(module = "领用中心", value = "撤回领用申请")
    @PutMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        applyService.cancel(id);
        return Result.success("已撤回", null);
    }

    /**
     * 审批列表（仓库角色视角）
     */
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER') and hasAuthority('warehouse:apply:audit')")
    @GetMapping("/page")
    public Result<PageResult<StaffApply>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) Integer status,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(applyService.auditPage(pageNum, pageSize, status, keyword));
    }

    /**
     * 通过申请（自动生成出库单）
     */
    @Log(module = "领用中心", value = "通过领用申请")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER') and hasAuthority('warehouse:apply:agree')")
    @PutMapping("/{id}/agree")
    public Result<Void> agree(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.get("remark");
        applyService.agree(id, remark);
        return Result.success("已通过，出库单已自动生成", null);
    }

    /**
     * 驳回申请
     */
    @Log(module = "领用中心", value = "驳回领用申请")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','MANAGER') and hasAuthority('warehouse:apply:reject')")
    @PutMapping("/{id}/reject")
    public Result<Void> reject(@PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        String remark = body == null ? null : body.get("remark");
        applyService.reject(id, remark);
        return Result.success("已驳回", null);
    }
}
