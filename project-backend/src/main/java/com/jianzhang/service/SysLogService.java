package com.jianzhang.service;

import com.jianzhang.common.PageResult;
import com.jianzhang.entity.SysLog;

/**
 * 系统日志服务
 */
public interface SysLogService {

    PageResult<SysLog> page(Integer pageNum, Integer pageSize, String keyword, String startTime, String endTime);

    void clear();

    /** 导出 Excel（沿用当前筛选条件） */
    void export(String keyword, String startTime, String endTime, javax.servlet.http.HttpServletResponse response)
            throws java.io.IOException;
}
