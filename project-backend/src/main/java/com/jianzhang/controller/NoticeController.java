package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.PageResult;
import com.jianzhang.common.Result;
import com.jianzhang.entity.SysNotice;
import com.jianzhang.service.SysNoticeService;
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
 * 通知公告接口
 */
@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final SysNoticeService noticeService;

    /**
     * 已发布公告（所有登录用户可见，用于看板公告栏）
     */
    @GetMapping("/api/notice/public")
    public Result<List<SysNotice>> published() {
        return Result.success(noticeService.published());
    }

    // ==================== 管理端 CRUD ====================

    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:notice:list')")
    @GetMapping("/api/admin/notices")
    public Result<PageResult<SysNotice>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String keyword) {
        return Result.success(noticeService.page(pageNum, pageSize, keyword));
    }

    @Log(module = "系统管理", value = "新增公告")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:notice:add')")
    @PostMapping("/api/admin/notices")
    public Result<Void> add(@RequestBody SysNotice notice) {
        noticeService.add(notice);
        return Result.success("发布成功", null);
    }

    @Log(module = "系统管理", value = "编辑公告")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:notice:edit')")
    @PutMapping("/api/admin/notices")
    public Result<Void> update(@RequestBody SysNotice notice) {
        noticeService.update(notice);
        return Result.success("保存成功", null);
    }

    @Log(module = "系统管理", value = "删除公告")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('system:notice:remove')")
    @DeleteMapping("/api/admin/notices/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        noticeService.delete(id);
        return Result.success("删除成功", null);
    }
}
