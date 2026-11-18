package com.jianzhang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.jianzhang.dto.RecordQuery;
import com.jianzhang.entity.WarehouseRecord;
import com.jianzhang.vo.StockVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 仓库出入库 Mapper
 */
@Mapper
public interface WarehouseRecordMapper extends BaseMapper<WarehouseRecord> {

    /** 分页 + 条件查询 */
    IPage<WarehouseRecord> selectPageList(IPage<WarehouseRecord> page, @Param("q") RecordQuery query);

    /** 导出用全量查询 */
    List<WarehouseRecord> selectExportList(@Param("q") RecordQuery query);

    /** 汇总：入库总量 / 出库总量 / 商品种类 / 当前总库存 */
    Map<String, Object> selectSummary();

    /** 近 N 个月出入库趋势 */
    List<Map<String, Object>> selectMonthlyTrend(@Param("months") int months);

    /** 库存台账：按商品汇总入库、出库、当前库存 */
    List<StockVO> selectGoodsStock(@Param("keyword") String keyword);

    /** 计算某商品当前库存（入库总量 - 出库总量） */
    Integer selectCurrentStock(@Param("goodsName") String goodsName);

    /** 商品名称列表（用于下拉提示） */
    List<String> selectGoodsNames();

    /** 回收站分页查询 */
    List<WarehouseRecord> selectRecyclePage(@Param("keyword") String keyword);

    /** 移入回收站（逻辑删除） */
    int markDeleted(@Param("id") Long id);

    /** 从回收站恢复 */
    int restoreById(@Param("id") Long id);

    /** 从回收站彻底删除 */
    int purgeById(@Param("id") Long id);
}
