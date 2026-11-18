package com.jianzhang.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商品档案表 warehouse_goods
 */
@Data
@TableName("warehouse_goods")
public class WarehouseGoods implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 商品ID */
    @TableId(value = "goods_id", type = IdType.AUTO)
    private Long goodsId;

    /** 商品名称（与出入库台账关联） */
    private String goodsName;

    /** 商品分类 */
    private String category;

    /** 计量单位 */
    private String unit;

    /** 参考单价(元) */
    private BigDecimal price;

    /** 库存预警阈值（低于则预警） */
    private Integer minStock;

    /** 状态 1在册 0停用 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
