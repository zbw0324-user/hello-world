package com.jianzhang.util;

import com.jianzhang.exception.BusinessException;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 日期时间工具
 */
public class DateUtils {

    public static final DateTimeFormatter DATETIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter MINUTE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DateUtils() {
    }

    /**
     * 兼容 yyyy-MM-dd / yyyy-MM-dd HH:mm / yyyy-MM-dd HH:mm:ss 三种格式
     */
    public static LocalDateTime parse(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("时间不能为空");
        }
        String text = value.trim();
        try {
            if (text.length() <= 10) {
                return LocalDate.parse(text).atStartOfDay();
            }
            if (text.length() <= 16) {
                return LocalDateTime.parse(text, MINUTE);
            }
            return LocalDateTime.parse(text, DATETIME);
        } catch (Exception e) {
            throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    /**
     * 宽松解析（用于 Excel 导入）：
     * 支持 2026-11-17 / 2026/11/17 / 2026.11.17，
     * 支持 09:30、9:30:00，支持末尾带 .0 的小数秒，也兼容 ISO 的 "T" 分隔符。
     */
    public static LocalDateTime parseLoose(String value) {
        if (!StringUtils.hasText(value)) {
            throw new BusinessException("时间不能为空");
        }
        String text = value.trim()
                .replace('/', '-')
                .replace('.', '-')
                .replace('T', ' ');
        // 去掉 Excel 可能带出的毫秒/小数秒，如 2026-11-17 09:30:00.0
        int dot = text.lastIndexOf('-');
        if (dot > 10) {
            text = text.substring(0, dot);
        }
        text = text.replaceAll("\\s+", " ").trim();
        // 处理 9:30 这类缺前导 0 的时间
        if (text.length() > 10) {
            String datePart = text.substring(0, 10);
            String timePart = text.substring(11).trim();
            String[] parts = timePart.split(":");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < parts.length; i++) {
                String part = parts[i].trim();
                if (i > 0) {
                    sb.append(':');
                }
                sb.append(part.length() == 1 ? "0" + part : part);
            }
            text = datePart + " " + sb;
        }
        try {
            if (text.length() <= 10) {
                return LocalDate.parse(text).atStartOfDay();
            }
            if (text.length() <= 16) {
                return LocalDateTime.parse(text, MINUTE);
            }
            if (text.length() == 19) {
                return LocalDateTime.parse(text, DATETIME);
            }
            // 仍不符合则截断到秒再试一次
            return LocalDateTime.parse(text.substring(0, 19), DATETIME);
        } catch (Exception e) {
            throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
        }
    }

    public static String format(LocalDateTime time) {
        return time == null ? "" : time.format(DATETIME);
    }
}
