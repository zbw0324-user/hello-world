package com.jianzhang.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.jianzhang.common.PageResult;
import com.jianzhang.entity.SysLog;
import com.jianzhang.entity.SysLog;
import com.jianzhang.mapper.SysLogMapper;
import com.jianzhang.service.SysLogService;
import com.jianzhang.util.DateUtils;
import com.jianzhang.util.ExcelUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 系统日志服务实现
 */
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper sysLogMapper;

    @Override
    public PageResult<SysLog> page(Integer pageNum, Integer pageSize, String keyword, String startTime, String endTime) {
        Page<SysLog> page = new Page<>(pageNum == null ? 1 : pageNum, pageSize == null ? 10 : pageSize);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysLog::getUsername, keyword)
                        .or()
                        .like(SysLog::getOperation, keyword)
                        .or()
                        .like(SysLog::getModule, keyword))
                .ge(StringUtils.hasText(startTime), SysLog::getCreateTime, parseStart(startTime))
                .le(StringUtils.hasText(endTime), SysLog::getCreateTime, parseEnd(endTime))
                .orderByDesc(SysLog::getLogId);
        Page<SysLog> result = sysLogMapper.selectPage(page, wrapper);
        return PageResult.of(result.getTotal(), result.getRecords());
    }

    @Override
    public void clear() {
        sysLogMapper.delete(new LambdaQueryWrapper<SysLog>().isNotNull(SysLog::getLogId));
    }

    @Override
    public void export(String keyword, String startTime, String endTime, javax.servlet.http.HttpServletResponse response)
            throws java.io.IOException {
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<SysLog>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysLog::getUsername, keyword)
                        .or()
                        .like(SysLog::getOperation, keyword)
                        .or()
                        .like(SysLog::getModule, keyword))
                .ge(StringUtils.hasText(startTime), SysLog::getCreateTime, parseStart(startTime))
                .le(StringUtils.hasText(endTime), SysLog::getCreateTime, parseEnd(endTime))
                .orderByDesc(SysLog::getLogId)
                .last("LIMIT 5000");
        List<SysLog> logs = sysLogMapper.selectList(wrapper);

        List<String> headers = java.util.Arrays.asList("日志ID", "操作账号", "角色", "模块", "操作内容", "请求方式", "请求地址", "IP", "耗时(ms)", "状态", "操作时间");
        List<List<Object>> rows = new java.util.ArrayList<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (SysLog log : logs) {
            rows.add(java.util.Arrays.asList(
                    log.getLogId(),
                    log.getUsername(),
                    log.getRoleCode(),
                    log.getModule(),
                    log.getOperation(),
                    log.getMethod(),
                    log.getRequestUri(),
                    log.getIp(),
                    log.getCostTime(),
                    log.getStatus() != null && log.getStatus() == 1 ? "成功" : "失败",
                    log.getCreateTime() == null ? "" : log.getCreateTime().format(formatter)));
        }
        ExcelUtil.export(response, "操作日志", headers, rows);
    }

    private LocalDateTime parseStart(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().length() <= 10
                ? LocalDate.parse(value.trim()).atStartOfDay()
                : DateUtils.parse(value);
    }

    private LocalDateTime parseEnd(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim().length() <= 10
                ? LocalDate.parse(value.trim()).atTime(23, 59, 59)
                : DateUtils.parse(value);
    }

    /**
     * 供导出使用：按条件查询全部日志
     */
    public List<SysLog> listForExport(String keyword) {
        return sysLogMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .and(StringUtils.hasText(keyword), w -> w
                        .like(SysLog::getUsername, keyword)
                        .or()
                        .like(SysLog::getOperation, keyword))
                .orderByDesc(SysLog::getLogId));
    }
}
