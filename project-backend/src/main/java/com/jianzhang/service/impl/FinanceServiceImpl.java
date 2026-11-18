package com.jianzhang.service.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzhang.common.PageResult;
import com.jianzhang.dto.FinanceDTO;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.entity.FinanceRecord;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.FinanceRecordMapper;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.service.FinanceService;
import com.jianzhang.util.DateUtils;
import com.jianzhang.util.ExcelUtil;
import com.jianzhang.vo.FinanceStatVO;
import com.jianzhang.vo.ImportResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 财务收支服务实现
 */
@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    /** 导入 / 导出统一表头，顺序不可随意调整 */
    private static final List<String> HEADERS = Collections.unmodifiableList(Arrays.asList(
            "收支类型", "金额(元)", "来源/用途", "收支时间", "备注"));

    private static final BigDecimal MAX_AMOUNT = new BigDecimal("99999999.99");

    /** 大额支出审批阈值：支出金额 >= 该值自动进入待审批 */
    public static final BigDecimal AUDIT_THRESHOLD = new BigDecimal("10000");

    private final FinanceRecordMapper financeRecordMapper;

    @Override
    public PageResult<FinanceRecord> page(RecordQuery query) {
        Page<FinanceRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<FinanceRecord> result = financeRecordMapper.selectPageList(page, query);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    public FinanceRecord detail(Long id) {
        FinanceRecord record = financeRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("收支记录不存在或已被删除");
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(FinanceDTO dto) {
        FinanceRecord record = new FinanceRecord();
        copy(dto, record);
        record.setAuditStatus(resolveAuditStatus(record));
        financeRecordMapper.insert(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(FinanceDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少记录ID");
        }
        FinanceRecord exist = financeRecordMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException("收支记录不存在或已被删除");
        }
        if (exist.getAuditStatus() != null && exist.getAuditStatus() == 1) {
            throw new BusinessException("该记录正在审批中，暂不可编辑，请等待管理层审批或驳回后再修改");
        }
        FinanceRecord record = new FinanceRecord();
        copy(dto, record);
        // 驳回记录修改后重新送审（金额仍超阈值），否则恢复为无需审批；同时清空原审批信息
        financeRecordMapper.update(new LambdaUpdateWrapper<FinanceRecord>()
                .eq(FinanceRecord::getId, dto.getId())
                .set(FinanceRecord::getType, record.getType())
                .set(FinanceRecord::getAmount, record.getAmount())
                .set(FinanceRecord::getSource, record.getSource())
                .set(FinanceRecord::getRecordTime, record.getRecordTime())
                .set(FinanceRecord::getRemark, record.getRemark())
                .set(FinanceRecord::getAuditStatus, resolveAuditStatus(record))
                .set(FinanceRecord::getAuditBy, null)
                .set(FinanceRecord::getAuditTime, null)
                .set(FinanceRecord::getAuditRemark, null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FinanceRecord exist = financeRecordMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("收支记录不存在或已被删除");
        }
        if (exist.getAuditStatus() != null && exist.getAuditStatus() == 1) {
            throw new BusinessException("该记录正在审批中，暂不可删除，请等待管理层审批或驳回");
        }
        // 移入回收站，可在回收站恢复或彻底删除
        financeRecordMapper.markDeleted(id);
    }

    @Override
    public PageResult<FinanceRecord> recyclePage(Integer pageNum, Integer pageSize, String keyword) {
        List<FinanceRecord> all = financeRecordMapper.selectRecyclePage(keyword);
        int from = (int) Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<FinanceRecord> records = from <= to ? all.subList(from, to) : new ArrayList<>();
        return PageResult.of((long) all.size(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        int rows = financeRecordMapper.restoreById(id);
        if (rows == 0) {
            throw new BusinessException("记录不在回收站中");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purge(Long id) {
        int rows = financeRecordMapper.purgeById(id);
        if (rows == 0) {
            throw new BusinessException("记录不在回收站中");
        }
    }

    @Override
    public FinanceStatVO stat(int months) {
        int range = months <= 0 ? 6 : months;
        FinanceStatVO vo = new FinanceStatVO();

        Map<String, Object> summary = financeRecordMapper.selectSummary(new RecordQuery());
        BigDecimal income = Convert.toBigDecimal(summary.get("totalIncome"), BigDecimal.ZERO);
        BigDecimal expense = Convert.toBigDecimal(summary.get("totalExpense"), BigDecimal.ZERO);
        vo.setTotalIncome(income);
        vo.setTotalExpense(expense);
        vo.setBalance(income.subtract(expense));
        vo.setRecordCount(Convert.toLong(summary.get("recordCount"), 0L));
        vo.setTrend(financeRecordMapper.selectMonthlyTrend(range));
        vo.setIncomeSources(financeRecordMapper.selectSourceSummary(1));
        vo.setExpenseSources(financeRecordMapper.selectSourceSummary(2));
        return vo;
    }

    @Override
    public void export(RecordQuery query, HttpServletResponse response) throws IOException {
        List<FinanceRecord> records = financeRecordMapper.selectExportList(query);
        List<List<Object>> rows = new ArrayList<>();
        for (FinanceRecord record : records) {
            rows.add(Arrays.asList(
                    record.getId(),
                    record.getType() != null && record.getType() == 1 ? "收入" : "支出",
                    record.getAmount(),
                    record.getSource(),
                    DateUtils.format(record.getRecordTime()),
                    record.getRemark()
            ));
        }
        List<String> exportHeaders = Arrays.asList("编号", "收支类型", "金额(元)", "来源/用途", "收支时间", "备注");
        ExcelUtil.export(response, "财务收支明细_" + timestamp(), exportHeaders, rows);
    }

    @Override
    public void template(HttpServletResponse response) throws IOException {
        List<List<Object>> samples = new ArrayList<>();
        samples.add(Arrays.asList("收入", 128000.00, "产品销售回款", "2026-03-05 10:30:00", "华东区回款"));
        samples.add(Arrays.asList("支出", 46000.00, "原材料采购", "2026-03-06 14:00:00", "钢材一批"));
        ExcelUtil.exportTemplate(response, "财务收支导入模板", HEADERS, samples,
                "「收支类型」填写 收入 / 支出（也支持 1 / 2）",
                "「金额(元)」为大于 0 的数字，最多两位小数，无需填写货币符号",
                "「来源/用途」必填，最多 100 个字符",
                "「收支时间」格式 yyyy-MM-dd HH:mm:ss，也支持 yyyy/MM/dd、yyyy-MM-dd",
                "「备注」选填，最多 255 个字符",
                "单次最多导入 " + ExcelUtil.MAX_IMPORT_ROWS + " 行，校验失败的行会被跳过并给出原因");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        ImportResultVO result = ImportResultVO.of();
        List<List<String>> rows = ExcelUtil.read(file);
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中没有可导入的数据，请使用系统模板填写");
        }
        // 首行是表头则跳过
        if (looksLikeHeader(rows.get(0))) {
            rows = rows.subList(1, rows.size());
        }
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中只有表头，没有数据行");
        }
        for (int i = 0; i < rows.size(); i++) {
            List<String> row = rows.get(i);
            int rowNo = i + 2;
            try {
                FinanceRecord record = convert(row);
                financeRecordMapper.insert(record);
                result.addSuccess();
            } catch (Exception e) {
                result.addFailure("第 " + rowNo + " 行：" + message(e));
            }
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO batchAdd(List<FinanceDTO> list) {
        ImportResultVO result = ImportResultVO.of();
        if (list == null || list.isEmpty()) {
            throw new BusinessException("请至少填写一行数据");
        }
        if (list.size() > ExcelUtil.MAX_IMPORT_ROWS) {
            throw new BusinessException("单次最多提交 " + ExcelUtil.MAX_IMPORT_ROWS + " 行");
        }
        for (int i = 0; i < list.size(); i++) {
            FinanceDTO dto = list.get(i);
            int rowNo = i + 1;
            try {
                if (dto == null) {
                    throw new BusinessException("整行为空");
                }
                if (dto.getType() == null || (dto.getType() != 1 && dto.getType() != 2)) {
                    throw new BusinessException("收支类型只能是 收入 / 支出");
                }
                if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new BusinessException("金额必须大于 0");
                }
                if (!StringUtils.hasText(dto.getSource())) {
                    throw new BusinessException("来源/用途不能为空");
                }
                if (!StringUtils.hasText(dto.getRecordTime())) {
                    throw new BusinessException("收支时间不能为空");
                }
                FinanceRecord record = new FinanceRecord();
                copy(dto, record);
                financeRecordMapper.insert(record);
                result.addSuccess();
            } catch (Exception e) {
                result.addFailure("第 " + rowNo + " 行：" + message(e));
            }
        }
        return result;
    }

    // ================= 大额支出审批 =================

    @Override
    public PageResult<FinanceRecord> auditPage(RecordQuery query) {
        // 审批中心默认只看待审批
        if (query.getAuditStatus() == null) {
            query.setAuditStatus(1);
        }
        return page(query);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void agreeAudit(Long id, String remark) {
        FinanceRecord record = mustPending(id);
        FinanceRecord update = new FinanceRecord();
        update.setId(id);
        update.setAuditStatus(2);
        update.setAuditBy(SecurityUtils.getUsername());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(StringUtils.hasText(remark) ? remark : "同意");
        financeRecordMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectAudit(Long id, String remark) {
        FinanceRecord record = mustPending(id);
        FinanceRecord update = new FinanceRecord();
        update.setId(id);
        update.setAuditStatus(3);
        update.setAuditBy(SecurityUtils.getUsername());
        update.setAuditTime(LocalDateTime.now());
        update.setAuditRemark(StringUtils.hasText(remark) ? remark : "驳回");
        financeRecordMapper.updateById(update);
    }

    @Override
    public Long countPendingAudit() {
        return financeRecordMapper.selectCount(new LambdaQueryWrapper<FinanceRecord>()
                .eq(FinanceRecord::getAuditStatus, 1));
    }

    private FinanceRecord mustPending(Long id) {
        FinanceRecord record = financeRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("收支记录不存在或已被删除");
        }
        if (record.getAuditStatus() == null || record.getAuditStatus() != 1) {
            throw new BusinessException("该记录不在待审批状态");
        }
        return record;
    }

    /**
     * 支出金额达到审批阈值则进入待审批，其余无需审批
     */
    private Integer resolveAuditStatus(FinanceRecord record) {
        if (record.getType() != null && record.getType() == 2
                && record.getAmount() != null
                && record.getAmount().compareTo(AUDIT_THRESHOLD) >= 0) {
            return 1;
        }
        return 0;
    }

    // ================= 内部方法 =================

    private boolean looksLikeHeader(List<String> first) {
        return !first.isEmpty() && first.get(0).replace(" ", "").contains("类型");
    }

    private FinanceRecord convert(List<String> row) {
        String typeText = cell(row, 0);
        String amountText = cell(row, 1);
        String source = cell(row, 2);
        String timeText = cell(row, 3);
        String remark = cell(row, 4);

        Integer type = parseType(typeText);
        if (type == null) {
            throw new BusinessException("收支类型只能是 收入 / 支出");
        }
        BigDecimal amount = parseAmount(amountText);
        if (!StringUtils.hasText(source)) {
            throw new BusinessException("来源/用途不能为空");
        }
        if (source.length() > 100) {
            throw new BusinessException("来源/用途不能超过 100 个字符");
        }
        if (remark.length() > 255) {
            throw new BusinessException("备注不能超过 255 个字符");
        }
        FinanceRecord record = new FinanceRecord();
        record.setType(type);
        record.setAmount(amount);
        record.setSource(source);
        record.setRecordTime(DateUtils.parseLoose(timeText));
        record.setRemark(StringUtils.hasText(remark) ? remark : null);
        return record;
    }

    private Integer parseType(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        if ("收入".equals(value) || "1".equals(value) || "income".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("支出".equals(value) || "2".equals(value) || "expense".equalsIgnoreCase(value)) {
            return 2;
        }
        return null;
    }

    private BigDecimal parseAmount(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("金额不能为空");
        }
        String value = text.replace(",", "").replace("￥", "").replace("¥", "").replace("元", "").trim();
        BigDecimal amount;
        try {
            amount = new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new BusinessException("金额格式不正确：" + text);
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("金额必须大于 0");
        }
        if (amount.compareTo(MAX_AMOUNT) > 0) {
            throw new BusinessException("金额超出上限（99999999.99）");
        }
        return amount.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String cell(List<String> row, int index) {
        return index < row.size() && row.get(index) != null ? row.get(index).trim() : "";
    }

    private String message(Exception e) {
        return StringUtils.hasText(e.getMessage()) ? e.getMessage() : "数据格式不正确";
    }

    private String timestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private void copy(FinanceDTO dto, FinanceRecord record) {
        record.setType(dto.getType());
        record.setAmount(dto.getAmount());
        record.setSource(dto.getSource().trim());
        record.setRecordTime(DateUtils.parse(dto.getRecordTime()));
        record.setRemark(dto.getRemark());
    }
}
