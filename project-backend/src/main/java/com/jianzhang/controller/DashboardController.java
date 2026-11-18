package com.jianzhang.controller;

import com.jianzhang.annotation.Log;
import com.jianzhang.common.Result;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.service.DashboardService;
import com.jianzhang.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 首页数据看板接口（所有角色可访问，数据按角色过滤）
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public Result<DashboardVO> dashboard() {
        return Result.success(dashboardService.getDashboard());
    }

    /**
     * 仅用于演示：返回当前查询参数结构，便于前端联调
     */
    @GetMapping("/ping")
    public Result<RecordQuery> ping() {
        return Result.success(new RecordQuery());
    }
}
