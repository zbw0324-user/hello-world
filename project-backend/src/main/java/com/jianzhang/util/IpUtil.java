package com.jianzhang.util;

import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;

/**
 * IP 工具类
 */
public class IpUtil {

    private static final String UNKNOWN = "unknown";

    private IpUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (isInvalid(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (isInvalid(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return "0:0:0:0:0:0:0:1".equals(ip) ? "127.0.0.1" : ip;
    }

    private static boolean isInvalid(String ip) {
        return !StringUtils.hasText(ip) || UNKNOWN.equalsIgnoreCase(ip);
    }
}
