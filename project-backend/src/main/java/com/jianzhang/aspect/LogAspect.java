package com.jianzhang.aspect;

import com.alibaba.fastjson2.JSON;
import com.jianzhang.annotation.Log;
import com.jianzhang.entity.SysLog;
import com.jianzhang.mapper.SysLogMapper;
import com.jianzhang.security.LoginUser;
import com.jianzhang.security.SecurityUtils;
import com.jianzhang.util.IpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * 操作日志切面：拦截 @Log 注解方法，记录操作人、参数、耗时、结果
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LogAspect {

    private final SysLogMapper sysLogMapper;

    @Around("@annotation(com.jianzhang.annotation.Log)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long start = System.currentTimeMillis();
        int status = 1;
        try {
            Object result = point.proceed();
            return result;
        } catch (Throwable throwable) {
            status = 0;
            throw throwable;
        } finally {
            try {
                saveLog(point, status, System.currentTimeMillis() - start);
            } catch (Exception e) {
                log.warn("操作日志记录失败：{}", e.getMessage());
            }
        }
    }

    private void saveLog(ProceedingJoinPoint point, int status, long cost) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Log annotation = signature.getMethod().getAnnotation(Log.class);
        if (annotation == null) {
            return;
        }

        SysLog sysLog = new SysLog();
        sysLog.setModule(annotation.module());
        sysLog.setOperation(annotation.value());
        sysLog.setMethod(signature.getDeclaringTypeName() + "." + signature.getName());
        sysLog.setStatus(status);
        sysLog.setCostTime(cost);

        LoginUser loginUser = SecurityUtils.getLoginUserOrNull();
        if (loginUser != null) {
            sysLog.setUsername(loginUser.getUsername());
            sysLog.setRoleCode(loginUser.getRoleCode());
        }

        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            sysLog.setRequestUri(request.getRequestURI());
            sysLog.setIp(IpUtil.getClientIp(request));
        }

        sysLog.setParams(buildParams(point.getArgs()));
        sysLogMapper.insert(sysLog);
    }

    /**
     * 序列化请求参数（过滤 Servlet 对象与文件，长度截断）
     */
    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return "{}";
        }
        List<Object> list = new ArrayList<>();
        for (Object arg : args) {
            if (arg instanceof HttpServletRequest || arg instanceof HttpServletResponse
                    || arg instanceof MultipartFile[]) {
                continue;
            }
            list.add(arg);
        }
        String json;
        try {
            json = JSON.toJSONString(list);
        } catch (Exception e) {
            return "[参数序列化失败]";
        }
        return json.length() > 900 ? json.substring(0, 900) + "..." : json;
    }
}
