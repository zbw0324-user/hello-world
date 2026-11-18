package com.jianzhang.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用列表查询参数（财务 / 仓库共用）
 */
@Data
public class RecordQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 页码 */
    private Integer pageNum = 1;

    /** 每页条数 */
    private Integer pageSize = 10;

    /** 类型：财务 1收入 2支出；仓库 1入库 2出库 */
    private Integer type;

    /** 关键词：财务匹配来源，仓库匹配商品名称 */
    private String keyword;

    /** 开始日期 yyyy-MM-dd */
    private String startTime;

    /** 结束日期 yyyy-MM-dd */
    private String endTime;

    /** 审批状态：0无需 1待审批 2通过 3驳回（财务专用，空则查全部） */
    private Integer auditStatus;

    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }
}
