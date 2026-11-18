package com.jianzhang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.entity.FinanceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 财务收支 Mapper
 */
@Mapper
public interface FinanceRecordMapper extends BaseMapper<FinanceRecord> {

    /** 分页 + 条件查询 */
    IPage<FinanceRecord> selectPageList(IPage<FinanceRecord> page, @Param("q") RecordQuery query);

    /** 导出用全量查询（不分页） */
    List<FinanceRecord> selectExportList(@Param("q") RecordQuery query);

    /** 汇总：总收入 / 总支出 / 记录数 */
    Map<String, Object> selectSummary(@Param("q") RecordQuery query);

    /** 近 N 个月收支趋势 */
    List<Map<String, Object>> selectMonthlyTrend(@Param("months") int months);

    /** 按来源/用途汇总（用于饼图） */
    List<Map<String, Object>> selectSourceSummary(@Param("type") Integer type);

    /** 回收站分页查询 */
    List<FinanceRecord> selectRecyclePage(@Param("keyword") String keyword);

    /** 移入回收站（逻辑删除） */
    int markDeleted(@Param("id") Long id);

    /** 从回收站恢复 */
    int restoreById(@Param("id") Long id);

    /** 从回收站彻底删除 */
    int purgeById(@Param("id") Long id);
}
