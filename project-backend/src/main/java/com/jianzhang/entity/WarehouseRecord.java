package com.jianzhang.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 仓库出入库表 warehouse_record
 */
@Data
@TableName("warehouse_record")
public class WarehouseRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 单据类型 1入库 2出库 */
    private Integer type;

    /** 商品名称 */
    private String goodsName;

    /** 数量 */
    private Integer goodsNum;

    /** 剩余库存 */
    private Integer stockRemain;

    /** 出入库时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTime;

    /** 操作人 */
    private String operator;

    /** 备注 */
    private String remark;

    /** 录入时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 逻辑删除 0正常 1回收站 */
    @TableLogic
    private Integer deleted;

    /** 删除时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deleteTime;
}
