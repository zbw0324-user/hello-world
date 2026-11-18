package com.jianzhang.service.impl;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzhang.common.PageResult;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.dto.WarehouseDTO;
import com.jianzhang.entity.WarehouseRecord;
import com.jianzhang.exception.BusinessException;
import com.jianzhang.mapper.WarehouseRecordMapper;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.service.WarehouseService;
import com.jianzhang.util.DateUtils;
import com.jianzhang.util.ExcelUtil;
import com.jianzhang.vo.ImportResultVO;
import com.jianzhang.vo.StockVO;
import com.jianzhang.vo.WarehouseStatVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 仓库出入库服务实现
 */
@Service
@RequiredArgsConstructor
public class WarehouseServiceImpl implements WarehouseService {

    /** 导入 / 导出统一表头，顺序不可随意调整 */
    private static final List<String> HEADERS = Collections.unmodifiableList(Arrays.asList(
            "单据类型", "商品名称", "数量", "出入库时间", "备注"));

    private final WarehouseRecordMapper warehouseRecordMapper;

    @Override
    public PageResult<WarehouseRecord> page(RecordQuery query) {
        Page<WarehouseRecord> page = new Page<>(query.getPageNum(), query.getPageSize());
        IPage<WarehouseRecord> result = warehouseRecordMapper.selectPageList(page, query);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    public WarehouseRecord detail(Long id) {
        WarehouseRecord record = warehouseRecordMapper.selectById(id);
        if (record == null) {
            throw new BusinessException("出入库单据不存在或已被删除");
        }
        return record;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(WarehouseDTO dto) {
        checkOutbound(dto);
        WarehouseRecord record = new WarehouseRecord();
        copy(dto, record);
        record.setOperator(SecurityUtils.getLoginUser().getNickname());
        warehouseRecordMapper.insert(record);
        recalcStock(record.getGoodsName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(WarehouseDTO dto) {
        if (dto.getId() == null) {
            throw new BusinessException("缺少单据ID");
        }
        WarehouseRecord exist = warehouseRecordMapper.selectById(dto.getId());
        if (exist == null) {
            throw new BusinessException("出入库单据不存在或已被删除");
        }
        checkOutbound(dto);
        WarehouseRecord record = new WarehouseRecord();
        record.setId(dto.getId());
        copy(dto, record);
        record.setOperator(SecurityUtils.getLoginUser().getNickname());
        warehouseRecordMapper.updateById(record);

        // 商品名称若被修改，新旧两个商品的库存都要重算
        recalcStock(exist.getGoodsName());
        if (!Objects.equals(exist.getGoodsName(), record.getGoodsName())) {
            recalcStock(record.getGoodsName());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        WarehouseRecord exist = warehouseRecordMapper.selectById(id);
        if (exist == null) {
            throw new BusinessException("出入库单据不存在或已被删除");
        }
        // 移入回收站，可在回收站恢复或彻底删除
        warehouseRecordMapper.markDeleted(id);
        recalcStock(exist.getGoodsName());
    }

    @Override
    public PageResult<WarehouseRecord> recyclePage(Integer pageNum, Integer pageSize, String keyword) {
        List<WarehouseRecord> all = warehouseRecordMapper.selectRecyclePage(keyword);
        int from = (int) Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(all.size(), from + pageSize);
        List<WarehouseRecord> records = from <= to ? all.subList(from, to) : new ArrayList<>();
        return PageResult.of((long) all.size(), records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        // 回收站中的单据（deleted=1）直接查，绕过逻辑删除过滤
        WarehouseRecord record = warehouseRecordMapper.selectRecyclePage(null).stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("单据不在回收站中"));
        // 出库单恢复前校验：当前库存 + 该单数量必须 >= 0
        Integer current = warehouseRecordMapper.selectCurrentStock(record.getGoodsName());
        int stockAfter = (current == null ? 0 : current)
                + (record.getType() != null && record.getType() == 1 ? record.getGoodsNum() : -record.getGoodsNum());
        if (stockAfter < 0) {
            throw new BusinessException("恢复失败：商品「" + record.getGoodsName() + "」当前库存不足（恢复后库存为 " + stockAfter + "）");
        }
        warehouseRecordMapper.restoreById(id);
        recalcStock(record.getGoodsName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void purge(Long id) {
        WarehouseRecord record = warehouseRecordMapper.selectRecyclePage(null).stream()
                .filter(r -> id.equals(r.getId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException("单据不在回收站中"));
        warehouseRecordMapper.purgeById(id);
    }

    @Override
    public List<StockVO> stockList(String keyword) {
        return warehouseRecordMapper.selectGoodsStock(keyword);
    }

    @Override
    public List<String> goodsNames() {
        return warehouseRecordMapper.selectGoodsNames();
    }

    @Override
    public WarehouseStatVO stat(int months) {
        int range = months <= 0 ? 6 : months;
        WarehouseStatVO vo = new WarehouseStatVO();
        Map<String, Object> summary = warehouseRecordMapper.selectSummary();
        vo.setTotalInNum(Convert.toInt(summary.get("totalInNum"), 0));
        vo.setTotalOutNum(Convert.toInt(summary.get("totalOutNum"), 0));
        vo.setGoodsKinds(Convert.toInt(summary.get("goodsKinds"), 0));
        vo.setTotalStock(Convert.toInt(summary.get("totalStock"), 0));
        vo.setTrend(warehouseRecordMapper.selectMonthlyTrend(range));
        vo.setStocks(warehouseRecordMapper.selectGoodsStock(null));
        return vo;
    }

    @Override
    public void export(RecordQuery query, HttpServletResponse response) throws IOException {
        List<WarehouseRecord> records = warehouseRecordMapper.selectExportList(query);
        List<List<Object>> rows = new ArrayList<>();
        for (WarehouseRecord record : records) {
            rows.add(Arrays.asList(
                    record.getId(),
                    record.getType() != null && record.getType() == 1 ? "入库" : "出库",
                    record.getGoodsName(),
                    record.getGoodsNum(),
                    record.getStockRemain(),
                    DateUtils.format(record.getRecordTime()),
                    record.getOperator(),
                    record.getRemark()
            ));
        }
        List<String> exportHeaders = Arrays.asList("编号", "单据类型", "商品名称", "数量", "剩余库存", "出入库时间", "操作人", "备注");
        ExcelUtil.export(response, "仓库出入库明细_" + timestamp(), exportHeaders, rows);
    }

    @Override
    public void template(HttpServletResponse response) throws IOException {
        List<List<Object>> samples = new ArrayList<>();
        samples.add(Arrays.asList("入库", "联想笔记本 T14", 20, "2026-03-05 09:00:00", "首批到货"));
        samples.add(Arrays.asList("出库", "联想笔记本 T14", 2, "2026-03-06 15:20:00", "技术部领用"));
        ExcelUtil.exportTemplate(response, "仓库出入库导入模板", HEADERS, samples,
                "「单据类型」填写 入库 / 出库（也支持 1 / 2）",
                "「商品名称」必填，最多 100 个字符；同名商品会自动进入同一台账",
                "「数量」为大于 0 的整数",
                "「出入库时间」格式 yyyy-MM-dd HH:mm:ss，也支持 yyyy/MM/dd、yyyy-MM-dd",
                "「备注」选填，最多 255 个字符",
                "出库行会按当前库存校验，库存不足的行会被跳过；导入完成后自动重算剩余库存",
                "单次最多导入 " + ExcelUtil.MAX_IMPORT_ROWS + " 行");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO importExcel(MultipartFile file) throws IOException {
        ImportResultVO result = ImportResultVO.of();
        List<List<String>> rows = ExcelUtil.read(file);
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中没有可导入的数据，请使用系统模板填写");
        }
        if (looksLikeHeader(rows.get(0))) {
            rows = rows.subList(1, rows.size());
        }
        if (rows.isEmpty()) {
            throw new BusinessException("Excel 中只有表头，没有数据行");
        }
        String operator = currentOperator();
        Set<String> affectedGoods = new LinkedHashSet<>();
        for (int i = 0; i < rows.size(); i++) {
            int rowNo = i + 2;
            try {
                WarehouseRecord record = convert(rows.get(i), operator);
                checkStock(record, null);
                warehouseRecordMapper.insert(record);
                affectedGoods.add(record.getGoodsName());
                result.addSuccess();
            } catch (Exception e) {
                result.addFailure("第 " + rowNo + " 行：" + message(e));
            }
        }
        affectedGoods.forEach(this::recalcStock);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImportResultVO batchAdd(List<WarehouseDTO> list) {
        ImportResultVO result = ImportResultVO.of();
        if (list == null || list.isEmpty()) {
            throw new BusinessException("请至少填写一行数据");
        }
        if (list.size() > ExcelUtil.MAX_IMPORT_ROWS) {
            throw new BusinessException("单次最多提交 " + ExcelUtil.MAX_IMPORT_ROWS + " 行");
        }
        String operator = currentOperator();
        Set<String> affectedGoods = new LinkedHashSet<>();
        for (int i = 0; i < list.size(); i++) {
            WarehouseDTO dto = list.get(i);
            int rowNo = i + 1;
            try {
                if (dto == null) {
                    throw new BusinessException("整行为空");
                }
                if (dto.getType() == null || (dto.getType() != 1 && dto.getType() != 2)) {
                    throw new BusinessException("单据类型只能是 入库 / 出库");
                }
                if (!StringUtils.hasText(dto.getGoodsName())) {
                    throw new BusinessException("商品名称不能为空");
                }
                if (dto.getGoodsNum() == null || dto.getGoodsNum() <= 0) {
                    throw new BusinessException("数量必须大于 0");
                }
                if (!StringUtils.hasText(dto.getRecordTime())) {
                    throw new BusinessException("出入库时间不能为空");
                }
                WarehouseRecord record = new WarehouseRecord();
                copy(dto, record);
                record.setOperator(operator);
                checkStock(record, null);
                warehouseRecordMapper.insert(record);
                affectedGoods.add(record.getGoodsName());
                result.addSuccess();
            } catch (Exception e) {
                result.addFailure("第 " + rowNo + " 行：" + message(e));
            }
        }
        affectedGoods.forEach(this::recalcStock);
        return result;
    }

    /**
     * 出库数量不能超过当前库存
     */
    private void checkOutbound(WarehouseDTO dto) {
        if (dto.getType() != null && dto.getType() == 2) {
            Integer current = warehouseRecordMapper.selectCurrentStock(dto.getGoodsName().trim());
            int stock = current == null ? 0 : current;
            // 编辑时需排除自身原有出库数量
            if (dto.getId() != null) {
                WarehouseRecord old = warehouseRecordMapper.selectById(dto.getId());
                if (old != null && Objects.equals(old.getType(), 2)
                        && Objects.equals(old.getGoodsName(), dto.getGoodsName().trim())) {
                    stock += old.getGoodsNum();
                }
            }
            if (stock < dto.getGoodsNum()) {
                throw new BusinessException("库存不足，当前可用库存为 " + stock);
            }
        }
    }

    /**
     * 导入 / 批量场景：按商品当前实时库存校验出库数量
     */
    private void checkStock(WarehouseRecord record, Long excludeId) {
        if (record.getType() == null || record.getType() != 2) {
            return;
        }
        Integer current = warehouseRecordMapper.selectCurrentStock(record.getGoodsName());
        int stock = current == null ? 0 : current;
        if (excludeId != null) {
            WarehouseRecord old = warehouseRecordMapper.selectById(excludeId);
            if (old != null && Objects.equals(old.getType(), 2)) {
                stock += old.getGoodsNum();
            }
        }
        if (stock < record.getGoodsNum()) {
            throw new BusinessException("「" + record.getGoodsName() + "」库存不足，当前可用 " + stock);
        }
    }

    /**
     * 重算某商品全部单据的剩余库存，保证台账数据一致
     */
    private void recalcStock(String goodsName) {
        if (goodsName == null) {
            return;
        }
        List<WarehouseRecord> records = warehouseRecordMapper.selectList(
                new LambdaQueryWrapper<WarehouseRecord>()
                        .eq(WarehouseRecord::getGoodsName, goodsName)
                        .orderByAsc(WarehouseRecord::getRecordTime)
                        .orderByAsc(WarehouseRecord::getId));
        int remain = 0;
        for (WarehouseRecord item : records) {
            remain += item.getType() != null && item.getType() == 1 ? item.getGoodsNum() : -item.getGoodsNum();
            if (!Objects.equals(item.getStockRemain(), remain)) {
                WarehouseRecord update = new WarehouseRecord();
                update.setId(item.getId());
                update.setStockRemain(remain);
                warehouseRecordMapper.updateById(update);
            }
        }
    }

    // ================= 导入解析 =================

    private boolean looksLikeHeader(List<String> first) {
        return !first.isEmpty() && first.get(0).replace(" ", "").contains("类型");
    }

    private WarehouseRecord convert(List<String> row, String operator) {
        String typeText = cell(row, 0);
        String goodsName = cell(row, 1);
        String numText = cell(row, 2);
        String timeText = cell(row, 3);
        String remark = cell(row, 4);

        Integer type = parseType(typeText);
        if (type == null) {
            throw new BusinessException("单据类型只能是 入库 / 出库");
        }
        if (!StringUtils.hasText(goodsName)) {
            throw new BusinessException("商品名称不能为空");
        }
        if (goodsName.length() > 100) {
            throw new BusinessException("商品名称不能超过 100 个字符");
        }
        if (remark.length() > 255) {
            throw new BusinessException("备注不能超过 255 个字符");
        }
        WarehouseRecord record = new WarehouseRecord();
        record.setType(type);
        record.setGoodsName(goodsName);
        record.setGoodsNum(parseNum(numText));
        record.setRecordTime(DateUtils.parseLoose(timeText));
        record.setOperator(operator);
        record.setRemark(StringUtils.hasText(remark) ? remark : null);
        return record;
    }

    private Integer parseType(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String value = text.trim();
        if ("入库".equals(value) || "1".equals(value) || "in".equalsIgnoreCase(value)) {
            return 1;
        }
        if ("出库".equals(value) || "2".equals(value) || "out".equalsIgnoreCase(value)) {
            return 2;
        }
        return null;
    }

    private Integer parseNum(String text) {
        if (!StringUtils.hasText(text)) {
            throw new BusinessException("数量不能为空");
        }
        String value = text.replace(",", "").replace("件", "").trim();
        try {
            // Excel 数字单元格可能被格式化为 20.0
            int num = new java.math.BigDecimal(value).intValue();
            if (num <= 0) {
                throw new BusinessException("数量必须大于 0");
            }
            return num;
        } catch (NumberFormatException e) {
            throw new BusinessException("数量格式不正确：" + text);
        }
    }

    private String currentOperator() {
        try {
            String nickname = SecurityUtils.getLoginUser().getNickname();
            return StringUtils.hasText(nickname) ? nickname : SecurityUtils.getLoginUser().getUsername();
        } catch (Exception e) {
            return "系统导入";
        }
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

    private void copy(WarehouseDTO dto, WarehouseRecord record) {
        record.setType(dto.getType());
        record.setGoodsName(dto.getGoodsName().trim());
        record.setGoodsNum(dto.getGoodsNum());
        record.setRecordTime(DateUtils.parse(dto.getRecordTime()));
        record.setRemark(dto.getRemark());
    }
}
