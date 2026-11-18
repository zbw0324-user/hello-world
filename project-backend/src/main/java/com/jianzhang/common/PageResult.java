package com.jianzhang.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分页响应结果
 *
 * @param <T> 行数据类型
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 总条数 */
    private Long total;
    /** 当前页数据 */
    private List<T> records;

    public static <T> PageResult<T> of(Long total, List<T> records) {
        return new PageResult<>(total == null ? 0L : total,
                records == null ? new ArrayList<>() : records);
    }
}
