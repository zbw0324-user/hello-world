package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jianzhang.entity.FinanceRecord;
import com.jianzhang.entity.WarehouseRecord;
import com.jianzhang.mapper.FinanceRecordMapper;
import com.jianzhang.mapper.WarehouseRecordMapper;
import com.jianzhang.security.LoginUser;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.service.DashboardService;
import com.jianzhang.service.FinanceService;
import com.jianzhang.service.StaffApplyService;
import com.jianzhang.service.WarehouseGoodsService;
import com.jianzhang.service.WarehouseService;
import com.jianzhang.vo.DashboardVO;
import com.jianzhang.vo.FinanceStatVO;
import com.jianzhang.vo.WarehouseStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 首页数据看板实现：按角色做数据级隔离
 */
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final String ROLE_ADMIN = "admin";
    private static final String ROLE_FINANCE = "finance";
    private static final String ROLE_WAREHOUSE = "warehouse";
    private static final String ROLE_MANAGER = "manager";

    private final FinanceService financeService;
    private final WarehouseService warehouseService;
    private final WarehouseGoodsService goodsService;
    private final StaffApplyService applyService;
    private final FinanceRecordMapper financeRecordMapper;
    private final WarehouseRecordMapper warehouseRecordMapper;

    @Override
    public DashboardVO getDashboard() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        String roleCode = loginUser.getRoleCode();
        DashboardVO vo = new DashboardVO();
        vo.setRoleCode(roleCode);

        // 管理层可同时查看财务与仓库概览；员工角色不返回业务数据，由前端展示个人面板
        boolean canFinance = ROLE_ADMIN.equals(roleCode) || ROLE_FINANCE.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
        boolean canWarehouse = ROLE_ADMIN.equals(roleCode) || ROLE_WAREHOUSE.equals(roleCode) || ROLE_MANAGER.equals(roleCode);

        if (canFinance) {
            FinanceStatVO stat = financeService.stat(6);
            vo.setTotalIncome(stat.getTotalIncome());
            vo.setTotalExpense(stat.getTotalExpense());
            vo.setBalance(stat.getBalance());
            vo.setRecordCount(stat.getRecordCount());
            vo.setFinanceTrend(stat.getTrend());
            vo.setRecentFinance(financeRecordMapper.selectList(new LambdaQueryWrapper<FinanceRecord>()
                    .orderByDesc(FinanceRecord::getRecordTime)
                    .orderByDesc(FinanceRecord::getId)
                    .last("LIMIT 5")));
        }

        if (canWarehouse) {
            vo.setLowStockCount(goodsService.countWarning());
            WarehouseStatVO stat = warehouseService.stat(6);
            vo.setTotalInNum(stat.getTotalInNum());
            vo.setTotalOutNum(stat.getTotalOutNum());
            vo.setGoodsKinds(stat.getGoodsKinds());
            vo.setTotalStock(stat.getTotalStock());
            vo.setStockTrend(stat.getTrend());
            vo.setRecentWarehouse(warehouseRecordMapper.selectList(new LambdaQueryWrapper<WarehouseRecord>()
                    .orderByDesc(WarehouseRecord::getRecordTime)
                    .orderByDesc(WarehouseRecord::getId)
                    .last("LIMIT 5")));
        }
        // 待办中心：管理层可审批大额支出；仓库/管理层可见领用待办；所有用户可见自己待审批的申请
        boolean canAuditFinance = ROLE_ADMIN.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
        boolean canAuditApply = ROLE_ADMIN.equals(roleCode) || ROLE_WAREHOUSE.equals(roleCode) || ROLE_MANAGER.equals(roleCode);
        if (canAuditFinance) {
            vo.setPendingFinanceAudit(financeService.countPendingAudit());
        }
        if (canAuditApply) {
            vo.setPendingApplyAudit(applyService.countPending());
        }
        vo.setMyPendingApply(applyService.countMyPending());
        return vo;
    }
}
