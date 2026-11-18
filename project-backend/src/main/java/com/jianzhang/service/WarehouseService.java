package com.jianzhang.service;

import com.jianzhang.common.PageResult;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.dto.WarehouseDTO;
import com.jianzhang.entity.WarehouseRecord;
import com.jianzhang.vo.ImportResultVO;
import com.jianzhang.vo.StockVO;
import com.jianzhang.vo.WarehouseStatVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 仓库出入库服务
 */
public interface WarehouseService {

    PageResult<WarehouseRecord> page(RecordQuery query);

    WarehouseRecord detail(Long id);

    void add(WarehouseDTO dto);

    void update(WarehouseDTO dto);

    void delete(Long id);

    /** 回收站分页 */
    PageResult<WarehouseRecord> recyclePage(Integer pageNum, Integer pageSize, String keyword);

    /** 从回收站恢复（出库单恢复前校验库存充足） */
    void restore(Long id);

    /** 彻底删除 */
    void purge(Long id);

    /**
     * 库存台账（按商品汇总）
     */
    List<StockVO> stockList(String keyword);

    /**
     * 商品名称列表
     */
    List<String> goodsNames();

    /**
     * 库存统计：出入库汇总 + 趋势 + 台账
     */
    WarehouseStatVO stat(int months);

    /**
     * 按筛选条件导出 Excel
     */
    void export(RecordQuery query, HttpServletResponse response) throws IOException;

    /**
     * 下载 Excel 导入模板
     */
    void template(HttpServletResponse response) throws IOException;

    /**
     * 从 Excel 导入出入库单据（自动校验库存并重算台账）
     */
    ImportResultVO importExcel(MultipartFile file) throws IOException;

    /**
     * 批量新增单据（前端可编辑表格提交）
     */
    ImportResultVO batchAdd(List<WarehouseDTO> list);
}
