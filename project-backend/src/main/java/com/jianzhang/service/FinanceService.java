package com.jianzhang.service;

import com.jianzhang.common.PageResult;
import com.jianzhang.dto.FinanceDTO;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.entity.FinanceRecord;
import com.jianzhang.vo.FinanceStatVO;
import com.jianzhang.vo.ImportResultVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 财务收支服务
 */
public interface FinanceService {

    PageResult<FinanceRecord> page(RecordQuery query);

    FinanceRecord detail(Long id);

    void add(FinanceDTO dto);

    void update(FinanceDTO dto);

    void delete(Long id);

    /** 回收站分页 */
    PageResult<FinanceRecord> recyclePage(Integer pageNum, Integer pageSize, String keyword);

    /** 从回收站恢复 */
    void restore(Long id);

    /** 彻底删除 */
    void purge(Long id);

    /**
     * 财务统计：汇总 + 近 N 个月趋势 + 来源占比
     */
    FinanceStatVO stat(int months);

    /**
     * 按当前筛选条件导出 Excel
     */
    void export(RecordQuery query, HttpServletResponse response) throws IOException;

    /**
     * 下载 Excel 导入模板
     */
    void template(HttpServletResponse response) throws IOException;

    /**
     * 从 Excel 导入收支记录（逐行校验，失败行跳过并返回原因）
     */
    ImportResultVO importExcel(MultipartFile file) throws IOException;

    /**
     * 批量新增（前端可编辑表格提交）
     */
    ImportResultVO batchAdd(List<FinanceDTO> list);

    // ==================== 大额支出审批 ====================

    /**
     * 待审批 / 已处理的支出记录分页（审批中心用）
     */
    PageResult<FinanceRecord> auditPage(RecordQuery query);

    /**
     * 审批通过
     */
    void agreeAudit(Long id, String remark);

    /**
     * 审批驳回
     */
    void rejectAudit(Long id, String remark);

    /**
     * 待审批记录数（看板用）
     */
    Long countPendingAudit();
}
