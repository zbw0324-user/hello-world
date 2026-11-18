package com.jianzhang.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量导入 / 批量新增结果
 */
@Data
public class ImportResultVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 提交（解析出）的总行数 */
    private int total;

    /** 成功条数 */
    private int success;

    /** 失败条数 */
    private int failed;

    /** 失败明细，最多保留 50 条，格式：第 N 行：原因 */
    private List<String> errors = new ArrayList<>();

    private static final int MAX_ERRORS = 50;

    public static ImportResultVO of() {
        return new ImportResultVO();
    }

    public void addSuccess() {
        this.total++;
        this.success++;
    }

    public void addFailure(String error) {
        this.total++;
        this.failed++;
        if (errors.size() < MAX_ERRORS) {
            errors.add(error);
        } else if (errors.size() == MAX_ERRORS) {
            errors.add("失败明细过多，仅展示前 " + MAX_ERRORS + " 条");
        }
    }

    /** 汇总一行可读文案，便于前端直接提示 */
    public String summary() {
        if (failed == 0) {
            return "成功处理 " + success + " 条数据";
        }
        return "成功 " + success + " 条，失败 " + failed + " 条";
    }
}
