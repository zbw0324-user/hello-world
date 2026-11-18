package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 财务统计报表数据
 */
@Data
public class FinanceStatVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总收入 */
    private BigDecimal totalIncome = BigDecimal.ZERO;
    /** 总支出 */
    private BigDecimal totalExpense = BigDecimal.ZERO;
    /** 结余 */
    private BigDecimal balance = BigDecimal.ZERO;
    /** 记录条数 */
    private Long recordCount = 0L;
    /** 近 N 月趋势 */
    private List<Map<String, Object>> trend = new ArrayList<>();
    /** 收入来源占比 */
    private List<Map<String, Object>> incomeSources = new ArrayList<>();
    /** 支出用途占比 */
    private List<Map<String, Object>> expenseSources = new ArrayList<>();
}
