package com.jianzhang;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * 简账云（JianZhang Cloud）启动类
 * 轻量级云端财务与进销存一体化管理平台
 *
 * @author jianzhang
 */
@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.jianzhang.mapper")
public class JianzhangApplication {

    public static void main(String[] args) {
        SpringApplication.run(JianzhangApplication.class, args);
        System.out.println("============== 简账云 JianZhang Cloud 启动成功 ==============");
        System.out.println("接口地址：http://localhost:8080/api");
        System.out.println("默认账号：admin / manager / finance / warehouse / staff，密码均为 123456");
        System.out.println("=================================================================");
    }
}
