package com.jianzhang.vo;

import com.jianzhang.entity.FinanceRecord;
import com.jianzhang.entity.WarehouseRecord;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 首页数据看板
 */
@Data
public class DashboardVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前角色，前端据此决定展示哪些卡片 */
    private String roleCode;

    private BigDecimal totalIncome = BigDecimal.ZERO;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal balance = BigDecimal.ZERO;
    private Long recordCount = 0L;

    private Integer totalInNum = 0;
    private Integer totalOutNum = 0;
    private Integer goodsKinds = 0;
    private Integer totalStock = 0;

    /** 近 6 个月财务趋势 */
    private List<Map<String, Object>> financeTrend = new ArrayList<>();
    /** 近 6 个月出入库趋势 */
    private List<Map<String, Object>> stockTrend = new ArrayList<>();

    private List<FinanceRecord> recentFinance = new ArrayList<>();
    private List<WarehouseRecord> recentWarehouse = new ArrayList<>();

    // ================= 待办中心（按角色填充） =================

    /** 待审批的大额支出数（管理层 / 管理员可见） */
    private Long pendingFinanceAudit = 0L;

    /** 待审批的领用申请数（仓库 / 管理层 / 管理员可见） */
    private Long pendingApplyAudit = 0L;

    /** 当前用户待审批的领用申请数（员工视角） */
    private Long myPendingApply = 0L;

    /** 库存预警商品数（仓库 / 管理层 / 管理员可见） */
    private Long lowStockCount = 0L;
}
