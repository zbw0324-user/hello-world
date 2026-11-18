package com.jianzhang.service;

import com.jianzhang.vo.DashboardVO;

/**
 * 首页数据看板服务
 */
public interface DashboardService {

    /**
     * 按当前登录角色返回可见的看板数据（财务看不到仓库，仓库看不到财务）
     */
    DashboardVO getDashboard();
}
